# Conector Semántico - OneCard

**Equipo:** Ctrl+C Ctrl+V

---

## 📋 Descripción del Problema

Los sistemas de gestión de inventarios y análisis de tickets de compra tradicionales enfrentan desafíos significativos:

- **Categorización manual ineficiente**: Clasificar miles de productos manualmente consume tiempo y es propenso a errores
- **Búsqueda limitada**: Los sistemas convencionales solo buscan coincidencias exactas en nombres, ignorando sinónimos o descripciones similares
- **Falta de inteligencia**: No pueden sugerir categorías automáticamente ni entender la intención del usuario
- **Análisis de tickets poco eficiente**: Procesar tickets de compra físicos requiere transcripción manual

Estos problemas resultan en:
- Pérdida de tiempo en tareas administrativas
- Dificultad para encontrar productos específicos
- Errores en la categorización de inventarios
- Análisis de datos de compra lento e impreciso

---

## 💡 Descripción de la Solución

**Conector Semántico** es una plataforma web inteligente que revoluciona la gestión de inventarios y tickets mediante:

### Características Principales

1. **Búsqueda Semántica Inteligente**
   - Utiliza algoritmos de procesamiento de lenguaje natural (TF-IDF) para encontrar productos por significado, no solo por coincidencia exacta
   - Entiende sinónimos y relaciones contextuales entre productos
   - Sugiere productos relacionados automáticamente

2. **Predicción Automática de Categorías**
   - Analiza la descripción del producto y sugiere la categoría más apropiada
   - Reduce errores de clasificación manual
   - Acelera el proceso de alta de productos

3. **Gestión de Tickets de Compra**
   - Carga de imágenes de tickets físicos
   - Visualización y análisis de tickets históricos
   - Estadísticas en tiempo real de gastos y patrones de compra

4. **Dashboard Analítico**
   - Métricas clave: total de tickets, gasto total, promedio por ticket
   - Visualización de productos y categorías
   - Interfaz intuitiva y responsive

### ¿Cómo Funciona?

El sistema opera en dos capas:

**Backend Java (Spring Boot)**
- Gestiona productos, categorías y tickets en PostgreSQL
- Proporciona interfaz web con Thymeleaf
- Maneja autenticación y seguridad

**Motor de IA Python (Flask)**
- API REST independiente en puerto 5000
- Algoritmo TF-IDF para vectorización de texto
- Cálculo de similitud coseno para búsqueda semántica
- Predicción de categorías por votación ponderada

**Flujo de Búsqueda:**
1. Usuario ingresa consulta (ej: "refresco de cola")
2. API Python normaliza el texto y lo vectoriza
3. Calcula similitud con todos los productos en base de datos
4. Retorna resultados ordenados por relevancia
5. Spring Boot presenta resultados en interfaz web

---

## 🚀 Instrucciones de Instalación y Uso

### Requisitos Previos

- **Java 21** o superior
- **PostgreSQL 12** o superior
- **Python 3.8** o superior
- **Maven 3.6** o superior
- Git

### Paso 1: Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/ingenieria_sin_limites_ctrl_c_ctrl_v.git
cd ingenieria_sin_limites_ctrl_c_ctrl_v
```

### Paso 2: Configurar Base de Datos

1. Crear base de datos PostgreSQL:
```sql
CREATE DATABASE "conector-semantico";
```

2. Poblar la base de datos con el script SQL incluido:
```bash
# Opción 1: Desde línea de comandos
psql -U postgres -d conector-semantico -f bd-sistema.sql

# Opción 2: Desde pgAdmin
# - Abrir pgAdmin
# - Conectar a la base de datos "conector-semantico"
# - Ir a Tools > Query Tool
# - Abrir el archivo bd-sistema.sql y ejecutarlo
```

**Nota:** El archivo [bd-sistema.sql](bd-sistema.sql) contiene todas las tablas, datos de prueba y configuraciones necesarias para que el sistema funcione correctamente.

3. Configurar credenciales en `team/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/conector-semantico
spring.datasource.username=postgres
spring.datasource.password=admin
```

### Paso 3: Configurar Backend Java (Spring Boot)

1. Navegar al directorio del proyecto:
```bash
cd team
```

2. Compilar el proyecto:
```bash
mvn clean install
```

3. Ejecutar la aplicación:
```bash
mvn spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8084`

### Paso 4: Configurar Motor de IA Python

1. Navegar al directorio Python:
```bash
cd python
```

2. Crear entorno virtual:
```bash
python -m venv venv
```

3. Activar entorno virtual:
- Windows:
```bash
venv\Scripts\activate
```
- Linux/Mac:
```bash
source venv/bin/activate
```

4. Instalar dependencias:
```bash
pip install -r requirements.txt
```

**Nota:** Las dependencias incluyen Flask, Flask-CORS, psycopg2-binary, NumPy, scikit-learn y pandas (opcional para carga de datos).

5. Cargar datos iniciales (opcional, solo si no usaste bd-sistema.sql):
```bash
python load_data_to_postgres.py
```

**Nota:** Si ya ejecutaste el archivo `bd-sistema.sql` en el Paso 2, puedes omitir este paso ya que la base de datos ya contiene todos los datos necesarios.

6. Iniciar API de búsqueda semántica:
```bash
python semantic_search_api.py
```

La API estará disponible en: `http://localhost:5000`

### Paso 5: Usar la Aplicación

1. Acceder a la aplicación web: `http://localhost:8084`
2. Navegar al dashboard para ver estadísticas
3. Usar la búsqueda semántica en `/busqueda-semantica`
4. Subir tickets en `/tickets/subir`

### Endpoints de la API Python

- `GET /api/health` - Verificar estado del servicio
- `GET /api/search?q=texto&limit=10` - Buscar productos similares
- `GET /api/predict-category?q=texto` - Predecir categoría
- `GET /api/smart-search?q=texto` - Búsqueda inteligente completa
- `POST /api/reload` - Recargar productos desde DB

**Ejemplo de uso:**
```bash
curl 'http://localhost:5000/api/search?q=coca%20cola&limit=5'
```

---

## 🛠️ Tecnologías Utilizadas

### Backend
- **Java 21** - Lenguaje de programación principal
- **Spring Boot 3.2.0** - Framework de aplicación
  - Spring Web - API REST y controladores
  - Spring Data JPA - Persistencia de datos
  - Spring Security - Autenticación y autorización
  - Thymeleaf - Motor de plantillas
- **Maven** - Gestor de dependencias
- **Lombok** - Reducción de código boilerplate
- **PostgreSQL** - Base de datos relacional

### Motor de IA
- **Python 3.x** - Lenguaje para procesamiento de ML
- **Flask** - Framework web para API REST
- **Flask-CORS** - Manejo de CORS
- **NumPy** - Operaciones numéricas
- **scikit-learn** - Algoritmos de ML
  - TfidfVectorizer - Vectorización de texto
  - cosine_similarity - Cálculo de similitud
- **psycopg2** - Conector PostgreSQL para Python

### Frontend
- **HTML5 + CSS3** - Estructura y estilos
- **Bootstrap 5** - Framework CSS responsivo
- **JavaScript (Vanilla)** - Interactividad del lado cliente
- **Thymeleaf** - Renderizado del lado servidor

### Herramientas de Desarrollo
- **Git** - Control de versiones
- **IntelliJ IDEA / VS Code** - IDEs
- **PostgreSQL Admin Tools** - Gestión de base de datos
- **Postman** - Testing de APIs

### Infraestructura
- **Servidor Embebido Tomcat** (Spring Boot)
- **Servidor Flask Development** (Python)
- **PostgreSQL Database Server**

---

## 📁 Estructura del Proyecto

```
ingenieria_sin_limites_ctrl_c_ctrl_v/
├── team/                              # Aplicación Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/hackathon/team/
│   │   │   │   ├── controller/       # Controladores MVC
│   │   │   │   ├── dao/              # Repositorios JPA
│   │   │   │   ├── model/            # Entidades JPA
│   │   │   │   └── config/           # Configuraciones
│   │   │   └── resources/
│   │   │       ├── templates/        # Vistas Thymeleaf
│   │   │       ├── static/           # CSS, JS, imágenes
│   │   │       └── application.properties
│   └── pom.xml                       # Dependencias Maven
├── python/                           # Motor de IA
│   ├── semantic_search_api.py        # API de búsqueda semántica (Flask)
│   ├── load_data_to_postgres.py      # Script de carga de datos (opcional)
│   ├── requirements.txt              # Dependencias Python
│   ├── data/                         # Datos CSV (opcional)
│   │   ├── DetalleFacturas.csv       # Dataset original
│   │   └── DetalleFacturas_clean.csv # Dataset limpio
│   ├── utils/                        # Utilidades
│   │   └── clean.ipynb               # Notebook para limpiar datos
│   └── venv/                         # Entorno virtual (ignorado en git)
├── bd-sistema.sql                    # Script SQL para poblar la BD
└── README.md                         # Este archivo
```

---

## 👥 Equipo de Desarrollo

- IGT - Juan José Rodríguez Contreras -1220197 
- ISC - Dylan Hernández Rojas - 1220143
- IET - Silemi Fragoso Olvera - 1210219
- ISC - Ana Laura Vidal López - 1180888
- ISC - José David García Verdugo - 1230362
- IIS- Wilken Alexander Núñez Orellana - 1230133
- IIS - Javier Alexis Aguirre Vasquez - 1240350
- ISC - Geisler Jiménez Torres - 1230572

---

## 📄 Licencia

Este proyecto fue desarrollado como parte del Hackathon OneCard 2024.
