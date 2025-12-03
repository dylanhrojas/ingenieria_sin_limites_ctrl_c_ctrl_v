#!/usr/bin/env python3
"""
API de Búsqueda Semántica de Productos
Usa embeddings para encontrar productos similares y sugerir categorías
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import psycopg2
from psycopg2.extras import RealDictCursor
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import re

app = Flask(__name__)
CORS(app)  # Permitir peticiones desde Spring Boot

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

# Cache global para productos y vectorizador
productos_cache = None
vectorizer = None
tfidf_matrix = None

def get_db_connection():
    """Obtiene conexión a la base de datos"""
    return psycopg2.connect(**DB_CONFIG, cursor_factory=RealDictCursor)

def load_products():
    """Carga todos los productos activos de la base de datos"""
    global productos_cache, vectorizer, tfidf_matrix

    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute("""
        SELECT
            p.id,
            p.nombre,
            p.marca,
            p.precio_referencia,
            c.id as categoria_id,
            c.nombre as categoria_nombre,
            c.palabras_clave as categoria_palabras
        FROM productos p
        INNER JOIN categoria c ON p.categoria_id = c.id
        WHERE p.activo = true AND c.activa = true
        ORDER BY p.id
    """)

    productos_cache = cursor.fetchall()

    conn.close()

    # Crear vectorizador TF-IDF
    if productos_cache:
        # Combinar nombre, marca y palabras clave de categoría para mejor búsqueda
        textos = []
        for p in productos_cache:
            texto = f"{p['nombre']} {p['marca'] or ''} {p['categoria_palabras'] or ''}"
            textos.append(texto.lower())

        vectorizer = TfidfVectorizer(
            ngram_range=(1, 2),  # Unigrams y bigrams
            min_df=1,
            stop_words=None
        )
        tfidf_matrix = vectorizer.fit_transform(textos)

    print(f"Productos cargados: {len(productos_cache)}")
    return productos_cache

def normalize_text(text):
    """Normaliza texto para búsqueda"""
    if not text:
        return ""

    # Convertir a minúsculas
    text = text.lower()

    # Remover caracteres especiales pero mantener espacios
    text = re.sub(r'[^a-záéíóúñ0-9\s]', ' ', text)

    # Remover espacios múltiples
    text = re.sub(r'\s+', ' ', text)

    return text.strip()

def search_similar_products(query, top_k=10):
    """
    Busca productos similares usando TF-IDF y cosine similarity

    Args:
        query (str): Texto de búsqueda
        top_k (int): Número de resultados a retornar

    Returns:
        list: Lista de productos similares con scores
    """
    global productos_cache, vectorizer, tfidf_matrix

    if not productos_cache:
        load_products()

    # Normalizar query
    query_normalized = normalize_text(query)

    if not query_normalized:
        return []

    # Vectorizar el query
    query_vector = vectorizer.transform([query_normalized])

    # Calcular similitud coseno
    similarities = cosine_similarity(query_vector, tfidf_matrix)[0]

    # Obtener índices ordenados por similitud
    top_indices = similarities.argsort()[::-1][:top_k]

    # Construir resultados
    results = []
    for idx in top_indices:
        score = float(similarities[idx])

        # Solo incluir si hay algo de similitud
        if score > 0:
            producto = dict(productos_cache[idx])
            producto['similarity_score'] = round(score, 4)
            producto['precio_referencia'] = float(producto['precio_referencia']) if producto['precio_referencia'] else None
            results.append(producto)

    # FALLBACK: Si no se encontró nada, mostrar productos aleatorios como sugerencias
    if len(results) == 0:
        import random
        # Tomar 10 productos aleatorios
        random_indices = random.sample(range(len(productos_cache)), min(top_k, len(productos_cache)))

        for idx in random_indices:
            producto = dict(productos_cache[idx])
            producto['similarity_score'] = 0.0  # Score 0 indica que es sugerencia aleatoria
            producto['precio_referencia'] = float(producto['precio_referencia']) if producto['precio_referencia'] else None
            producto['is_fallback'] = True  # Marcar como resultado de fallback
            results.append(producto)

    return results

def predict_category(query):
    """
    Predice la categoría más probable para una búsqueda

    Args:
        query (str): Texto de búsqueda

    Returns:
        dict: Categoría predicha con score
    """
    # Buscar productos similares
    similar_products = search_similar_products(query, top_k=20)

    if not similar_products:
        return None

    # Contar votos por categoría ponderados por similitud
    category_scores = {}

    for product in similar_products:
        cat_id = product['categoria_id']
        cat_name = product['categoria_nombre']
        score = product['similarity_score']

        if cat_id not in category_scores:
            category_scores[cat_id] = {
                'categoria_id': cat_id,
                'categoria_nombre': cat_name,
                'score': 0,
                'count': 0
            }

        category_scores[cat_id]['score'] += score
        category_scores[cat_id]['count'] += 1

    # Ordenar por score
    categories = sorted(
        category_scores.values(),
        key=lambda x: x['score'],
        reverse=True
    )

    return categories

# =====================================================
# ENDPOINTS DE LA API
# =====================================================

@app.route('/api/health', methods=['GET'])
def health():
    """Endpoint de salud"""
    return jsonify({
        'status': 'ok',
        'productos_cargados': len(productos_cache) if productos_cache else 0
    })

@app.route('/api/search', methods=['GET', 'POST'])
def search():
    """
    Busca productos similares

    Query params:
        q: Texto de búsqueda
        limit: Número máximo de resultados (default: 10)

    Ejemplo:
        GET /api/search?q=coca cola&limit=5
    """
    # Obtener query
    if request.method == 'POST':
        data = request.get_json()
        query = data.get('q', '') or data.get('query', '')
        limit = data.get('limit', 10)
    else:
        query = request.args.get('q', '') or request.args.get('query', '')
        limit = request.args.get('limit', 10, type=int)

    if not query:
        return jsonify({
            'error': 'El parámetro "q" es requerido'
        }), 400

    # Limitar resultados
    limit = min(limit, 50)  # Máximo 50

    # Buscar productos similares
    results = search_similar_products(query, top_k=limit)

    return jsonify({
        'query': query,
        'total_results': len(results),
        'results': results
    })

@app.route('/api/predict-category', methods=['GET', 'POST'])
def predict_category_endpoint():
    """
    Predice la categoría más probable para una búsqueda

    Query params:
        q: Texto de búsqueda

    Ejemplo:
        GET /api/predict-category?q=refresco
    """
    # Obtener query
    if request.method == 'POST':
        data = request.get_json()
        query = data.get('q', '') or data.get('query', '')
    else:
        query = request.args.get('q', '') or request.args.get('query', '')

    if not query:
        return jsonify({
            'error': 'El parámetro "q" es requerido'
        }), 400

    # Predecir categoría
    categories = predict_category(query)

    if not categories:
        return jsonify({
            'query': query,
            'prediccion': None,
            'todas_categorias': []
        })

    return jsonify({
        'query': query,
        'prediccion': categories[0] if categories else None,
        'todas_categorias': categories
    })

@app.route('/api/smart-search', methods=['GET', 'POST'])
def smart_search():
    """
    Búsqueda inteligente: combina productos similares y predicción de categoría

    Query params:
        q: Texto de búsqueda
        limit: Número máximo de productos (default: 10)

    Ejemplo:
        GET /api/smart-search?q=coca cola
    """
    # Obtener query
    if request.method == 'POST':
        data = request.get_json()
        query = data.get('q', '') or data.get('query', '')
        limit = data.get('limit', 10)
    else:
        query = request.args.get('q', '') or request.args.get('query', '')
        limit = request.args.get('limit', 10, type=int)

    if not query:
        return jsonify({
            'error': 'El parámetro "q" es requerido'
        }), 400

    # Limitar resultados
    limit = min(limit, 50)

    # Buscar productos y predecir categoría
    productos = search_similar_products(query, top_k=limit)
    categorias = predict_category(query)

    return jsonify({
        'query': query,
        'categoria_predicha': categorias[0] if categorias else None,
        'categorias_sugeridas': categorias[:3] if categorias else [],
        'productos_similares': productos,
        'total_productos': len(productos)
    })

@app.route('/api/reload', methods=['POST'])
def reload_products():
    """Recarga los productos desde la base de datos"""
    try:
        load_products()
        return jsonify({
            'status': 'ok',
            'message': 'Productos recargados exitosamente',
            'total': len(productos_cache) if productos_cache else 0
        })
    except Exception as e:
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500

# =====================================================
# INICIALIZACIÓN
# =====================================================

if __name__ == '__main__':
    print("Iniciando API de Búsqueda Semántica...")
    print("Cargando productos...")
    load_products()
    print(f"Productos cargados: {len(productos_cache)}")
    print("\nAPI corriendo en http://localhost:5000")
    print("\nEndpoints disponibles:")
    print("  GET  /api/health              - Estado del servicio")
    print("  GET  /api/search?q=texto      - Buscar productos similares")
    print("  GET  /api/predict-category?q=texto - Predecir categoría")
    print("  GET  /api/smart-search?q=texto - Búsqueda inteligente completa")
    print("  POST /api/reload              - Recargar productos desde DB")
    print("\nEjemplos:")
    print("  curl 'http://localhost:5000/api/search?q=coca%20cola&limit=5'")
    print("  curl 'http://localhost:5000/api/predict-category?q=refresco'")
    print("  curl 'http://localhost:5000/api/smart-search?q=galletas'")

    app.run(debug=True, host='0.0.0.0', port=5000)
