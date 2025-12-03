#!/usr/bin/env python3
"""
Script para cargar datos de CSVs a PostgreSQL
Migra categorías y productos a la base de datos conector-semantico
"""

import pandas as pd
import psycopg2
from psycopg2.extras import execute_values
from decimal import Decimal
import re

# =====================================================
# CONFIGURACIÓN DE BASE DE DATOS
# =====================================================
DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'conector-semantico',
    'user': 'postgres',
    'password': 'admin'
}

# =====================================================
# MAPEO DE CATEGORÍAS
# =====================================================
CATEGORY_MAPPING = {
    'food': {
        'nombre': 'Alimentos',
        'descripcion': 'Productos alimenticios y comestibles',
        'palabras_clave': 'comida, alimentos, bebidas, snacks, pan, dulces, botanas'
    },
    'alcohol': {
        'nombre': 'Bebidas Alcohólicas',
        'descripcion': 'Bebidas con contenido alcohólico',
        'palabras_clave': 'cerveza, vino, licor, alcohol, bebidas alcohólicas'
    },
    'tobacco': {
        'nombre': 'Tabaco',
        'descripcion': 'Productos de tabaco y afines',
        'palabras_clave': 'cigarro, tabaco, cigarrillos, vaper'
    },
    'product': {
        'nombre': 'Productos Generales',
        'descripcion': 'Productos diversos y generales',
        'palabras_clave': 'productos, artículos, mercancía'
    },
    'fuel': {
        'nombre': 'Combustibles',
        'descripcion': 'Gasolina, diesel y combustibles',
        'palabras_clave': 'gasolina, diesel, combustible, magna, premium'
    },
    'other': {
        'nombre': 'Otros',
        'descripcion': 'Otros productos no clasificados',
        'palabras_clave': 'varios, otros, misceláneos'
    }
}

def clean_price(price_value):
    """Limpia y convierte valores de precio a Decimal"""
    if pd.isna(price_value):
        return None

    try:
        # Convertir a string y limpiar
        price_str = str(price_value).strip()
        # Remover caracteres no numéricos excepto punto y coma
        price_str = re.sub(r'[^\d.,]', '', price_str)
        # Reemplazar coma por punto
        price_str = price_str.replace(',', '.')

        if price_str:
            return Decimal(price_str)
    except:
        pass

    return None

def clean_text(text):
    """Limpia texto removiendo caracteres especiales y limitando longitud"""
    if pd.isna(text):
        return None

    text = str(text).strip()
    # Remover saltos de línea y espacios múltiples
    text = re.sub(r'\s+', ' ', text)
    return text[:500] if text else None

def extract_brand(description):
    """Intenta extraer la marca del nombre del producto"""
    if not description:
        return None

    # Patrones comunes de marcas
    brands = [
        'Coca Cola', 'Pepsi', 'Sabritas', 'Marinela', 'Bimbo',
        'Lala', 'Danone', 'Nestlé', 'Herdez', 'Del Valle',
        'Bonafont', 'Ciel', 'Epura', 'Boing', 'Jumex',
        'Doritos', 'Cheetos', 'Ruffles', 'Energizer'
    ]

    desc_upper = description.upper()
    for brand in brands:
        if brand.upper() in desc_upper:
            return brand

    # Si no encuentra marca conocida, tomar primera palabra si es capitalizada
    words = description.split()
    if words and words[0][0].isupper():
        return words[0][:200]

    return None

def connect_db():
    """Conecta a la base de datos PostgreSQL"""
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        print("OK - Conexion exitosa a PostgreSQL")
        return conn
    except Exception as e:
        print(f"ERROR - Error al conectar a PostgreSQL: {e}")
        raise

def insert_categories(conn):
    """Inserta las categorías en la base de datos"""
    cursor = conn.cursor()

    print("\nInsertando categorias...")

    category_ids = {}

    for cat_key, cat_data in CATEGORY_MAPPING.items():
        try:
            # Insertar categoría (ON CONFLICT para evitar duplicados)
            cursor.execute("""
                INSERT INTO categoria (nombre, descripcion, palabras_clave, nivel, activa)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (nombre) DO UPDATE
                SET descripcion = EXCLUDED.descripcion,
                    palabras_clave = EXCLUDED.palabras_clave
                RETURNING id
            """, (
                cat_data['nombre'],
                cat_data['descripcion'],
                cat_data['palabras_clave'],
                1,  # nivel
                True  # activa
            ))

            cat_id = cursor.fetchone()[0]
            category_ids[cat_key] = cat_id
            print(f"   OK - {cat_data['nombre']} (ID: {cat_id})")

        except Exception as e:
            print(f"   ERROR - Error insertando {cat_data['nombre']}: {e}")
            conn.rollback()
            raise

    conn.commit()
    print(f"OK - {len(category_ids)} categorias insertadas/actualizadas")

    return category_ids

def load_products_from_csv(csv_path, category_ids, conn):
    """Carga productos desde CSV a la base de datos"""

    print(f"\nLeyendo CSV: {csv_path}")

    # Leer CSV con encoding correcto
    try:
        df = pd.read_csv(csv_path, encoding='utf-8')
    except UnicodeDecodeError:
        try:
            df = pd.read_csv(csv_path, encoding='latin-1')
        except:
            df = pd.read_csv(csv_path, encoding='cp1252')

    print(f"   Registros leidos: {len(df)}")

    # Filtrar registros válidos
    df = df[df['is_valid'] == True].copy()
    print(f"   Registros validos: {len(df)}")

    # Remover duplicados por descripción
    df = df.drop_duplicates(subset=['vcDescripcion'], keep='first')
    print(f"   Registros unicos: {len(df)}")

    cursor = conn.cursor()

    print("\nInsertando productos...")

    inserted_count = 0
    skipped_count = 0

    for idx, row in df.iterrows():
        # Extraer datos
        nombre = clean_text(row['vcDescripcion'])
        precio = clean_price(row['decImporte'])
        tipo = str(row['type']).lower().strip()

        # Validar datos mínimos
        if not nombre or len(nombre) < 2:
            skipped_count += 1
            continue

        # Obtener ID de categoría
        categoria_id = category_ids.get(tipo, category_ids.get('other'))

        # Extraer marca
        marca = extract_brand(nombre)

        # Insertar producto
        try:
            cursor.execute("""
                INSERT INTO productos (nombre, marca, precio_referencia, categoria_id, activo, fecha_creacion)
                VALUES (%s, %s, %s, %s, %s, NOW())
            """, (
                nombre,
                marca,
                precio,
                categoria_id,
                True
            ))

            inserted_count += 1
            if inserted_count % 100 == 0:
                print(f"   Procesados: {inserted_count}...")
                conn.commit()

        except Exception as e:
            print(f"   ADVERTENCIA - Error en fila {idx}: {e}")
            conn.rollback()
            skipped_count += 1
            continue

    conn.commit()

    print(f"\nOK - Productos insertados: {inserted_count}")
    print(f"INFO - Productos omitidos: {skipped_count}")

    return inserted_count

def main():
    """Función principal"""
    print("=" * 60)
    print("CARGA DE DATOS A POSTGRESQL")
    print("=" * 60)

    # Conectar a la base de datos
    conn = connect_db()

    try:
        # 1. Insertar categorías
        category_ids = insert_categories(conn)

        # 2. Cargar productos desde DetalleFacturas_clean.csv
        csv_path = 'data/DetalleFacturas_clean.csv'
        inserted = load_products_from_csv(csv_path, category_ids, conn)

        print("\n" + "=" * 60)
        print("PROCESO COMPLETADO EXITOSAMENTE")
        print("=" * 60)

        # Mostrar estadísticas finales
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM categoria WHERE activa = true")
        cat_count = cursor.fetchone()[0]

        cursor.execute("SELECT COUNT(*) FROM productos WHERE activo = true")
        prod_count = cursor.fetchone()[0]

        print(f"\nESTADISTICAS:")
        print(f"   Categorias activas: {cat_count}")
        print(f"   Productos activos: {prod_count}")

    except Exception as e:
        print(f"\nERROR: {e}")
        conn.rollback()
        raise

    finally:
        conn.close()
        print("\nConexion cerrada")

if __name__ == "__main__":
    main()
