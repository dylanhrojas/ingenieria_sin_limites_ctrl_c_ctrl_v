import numpy as np
import os
import uvicorn
from dotenv import load_dotenv

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from pydantic import BaseModel
from contextlib import asynccontextmanager

from services.embeddings import generate_embeddings, cosine_similarity
from services.search import load_texts, generate_all_embeddings, search, suggest_category, save_embeddings, load_embeddings

load_dotenv()

PORT = int(os.getenv("PORT", 8081))
HOST = os.getenv("HOST", "127.0.0.1")
DATA_PATH = os.getenv("DATA_PATH", "data/embeddings")

TEXTS = []
EMBEDDINGS = []
CATEGORIES = ["agua", "botana", "lácteos", "bebidas energéticas", "electrónica", "hogar"]
IDS = []
CATEGORY_EMBS = generate_embeddings(CATEGORIES)
DYNAMIC_CATEGORIES = []
DYNAMIC_CATEGORY_EMBS = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global TEXTS, EMBEDDINGS, IDS, DYNAMIC_CATEGORIES, DYNAMIC_CATEGORY_EMBS

    print("Intentando cargar embeddings guardados...")
    result = load_embeddings(DATA_PATH)

    if result["success"]:
        # Si hay embeddings guardados, cargarlos
        EMBEDDINGS = result["embeddings"]
        TEXTS = result["metadata"]["texts"]
        IDS = result["metadata"]["ids"]

        if "categories" in result["metadata"] and result["metadata"]["categories"]:
            DYNAMIC_CATEGORIES = list(set(result["metadata"]["categories"]))
            DYNAMIC_CATEGORY_EMBS = generate_embeddings(DYNAMIC_CATEGORIES)

        print(result["message"])
    else:
        # Si no hay embeddings guardados, cargar desde CSV
        print(result["message"])
        print("Cargando textos desde CSV...")
        TEXTS, loaded_categories = load_texts()

        print("Generando embeddings iniciales...")
        EMBEDDINGS, _ = generate_all_embeddings(TEXTS)

        # Generar IDs temporales (1, 2, 3, ...)
        IDS = list(range(1, len(TEXTS) + 1))

        # Actualizar categorías dinámicas si existen
        if loaded_categories:
            DYNAMIC_CATEGORIES = list(set([c for c in loaded_categories if c]))
            if DYNAMIC_CATEGORIES:
                DYNAMIC_CATEGORY_EMBS = generate_embeddings(DYNAMIC_CATEGORIES)

        # Guardar embeddings generados
        print("Guardando embeddings...")
        save_result = save_embeddings(
            embeddings=EMBEDDINGS,
            metadata={
                "ids": IDS,
                "texts": TEXTS,
                "categories": loaded_categories
            },
            filepath=DATA_PATH
        )
        print(save_result["message"])

    print(f"Motor semántico listo con {len(TEXTS)} items.")

    yield

    print("Cerrando microservicio...")

app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class SearchQuery(BaseModel):
    query: str
    top_k: int = 3

class EmbedInput(BaseModel):
    text: str

class ProductInput(BaseModel):
    text: str

class Product(BaseModel):
    id: int
    nombre: str
    categoria: str = ""
    precio: float

class BulkLoadInput(BaseModel):
    products: list[Product]

# ---- Endpoints ----
@app.post("/search")
def search_items(payload: SearchQuery):
    results = search(payload.query, EMBEDDINGS, TEXTS, payload.top_k)

    for result in results:
        text_index = TEXTS.index(result["text"])
        result["id"] = IDS[text_index]

    return {
        "query" : payload.query,
        "results" : results
    }

@app.post("/embed")
def embed_text(payload: EmbedInput):
    vec = generate_embeddings(payload.text)[0]
    return {"embedding": vec.tolist()}

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/add_item")
def add_item(payload: EmbedInput):
    global EMBEDDINGS, TEXTS, IDS
    vec = generate_embeddings(payload.text)[0]
    EMBEDDINGS = np.vstack([EMBEDDINGS, vec.reshape(1, -1)])
    TEXTS.append(payload.text)

    new_id = max(IDS) + 1 if IDS else 1
    IDS.append(new_id)

    save_result = save_embeddings(
        embeddings=EMBEDDINGS,
        metadata={
            "ids": IDS,
            "texts": TEXTS,
            "categories": []
        },
        filepath=DATA_PATH
    )

    return {
        "status": "ok",
        "product_added": payload.text,
        "id": new_id,
        "saved_to_disk": save_result["success"]
    }

@app.post("/save")
def save_data():
    """
    Guardar embeddings manualmente a disco.
    """
    result = save_embeddings(
        embeddings=EMBEDDINGS,
        metadata={
            "ids": IDS,
            "texts": TEXTS,
            "categories": []
        },
        filepath=DATA_PATH
    )
    return result

@app.post("/reload")
def reload_data():
    """
    Recargar embeddings desde disco
    """
    global EMBEDDINGS, TEXTS, IDS
    
    result = load_embeddings(DATA_PATH)
    
    if result["success"]:
        EMBEDDINGS = result["embeddings"]
        TEXTS = result["metadata"]["texts"]
        IDS = result["metadata"]["ids"]
        
        # Retornar solo info serializable
        return {
            "success": True,
            "message": result["message"],
            "count": len(EMBEDDINGS),
            "dimension": EMBEDDINGS.shape[1] if len(EMBEDDINGS) > 0 else 0
        }
    else:
        return {
            "success": False,
            "message": result["message"]
        }
    
@app.post("/bulk_load")
def bulk_load(payload: BulkLoadInput):
    """
    Carga masiva de productos desde Java.
    Reemplaza todos los embeddings existentes.
    """
    global EMBEDDINGS, TEXTS, IDS, DYNAMIC_CATEGORIES, DYNAMIC_CATEGORY_EMBS

    if not payload.products:
        return {
            "success": False,
            "message": "No se enviaron productos"
        }

    print(f"Cargando {len(payload.products)} productos...")

    # Extraer datos de los productos
    new_ids = []
    new_texts = []

    for product in payload.products:
        new_ids.append(product.id)
        # Combinar nombre y categoría para mejor contexto semántico
        text = f"{product.nombre} {product.categoria}"
        new_texts.append(text)

    # Generar embeddings
    print("Generando embeddings...")
    new_embeddings, _ = generate_all_embeddings(new_texts)

    # Actualizar variables globales
    EMBEDDINGS = new_embeddings
    TEXTS = new_texts
    IDS = new_ids

    # Actualizar categorías dinámicas
    DYNAMIC_CATEGORIES = list(set([p.categoria for p in payload.products]))
    DYNAMIC_CATEGORY_EMBS = generate_embeddings(DYNAMIC_CATEGORIES)
    
    # Guardar a disco
    print("Guardando embeddings...")
    save_result = save_embeddings(
        embeddings=EMBEDDINGS,
        metadata={
            "ids": IDS,
            "texts": TEXTS,
            "categories": [p.categoria for p in payload.products]
        },
        filepath=DATA_PATH
    )
    
    return {
        "success": True,
        "message": f"Cargados {len(payload.products)} productos exitosamente",
        "count": len(EMBEDDINGS),
        "saved_to_disk": save_result["success"]
    }


@app.post("/suggest_category")
def suggest_category_endpoint(payload: ProductInput):
    global DYNAMIC_CATEGORIES, DYNAMIC_CATEGORY_EMBS

    # Si hay categorías dinámicas cargadas, usarlas
    if DYNAMIC_CATEGORIES and DYNAMIC_CATEGORY_EMBS is not None:
        product_emb = generate_embeddings(payload.text)[0]
        scores = [cosine_similarity(product_emb, c_emb) for c_emb in DYNAMIC_CATEGORY_EMBS]
        best_idx = scores.index(max(scores))

        return {
            "product": payload.text,
            "category": DYNAMIC_CATEGORIES[best_idx],
            "score": float(scores[best_idx])
        }
    else:
        # Fallback a categorías por defecto
        result = suggest_category(payload.text)
        return result

if __name__ == "__main__":
    uvicorn.run(app, host=HOST, port=PORT)