import numpy as np

from fastapi import FastAPI
from pydantic import BaseModel
from contextlib import asynccontextmanager

from services.embeddings import generate_embeddings, cosine_similarity
from services.search import load_texts, generate_all_embeddings, search, suggest_category

TEXTS = []
EMBEDDINGS = []
CATEGORIES = ["agua", "botana", "lácteos", "bebidas energéticas", "electrónica", "hogar"]
CATEGORY_EMBS = generate_embeddings(CATEGORIES)

@asynccontextmanager
async def lifespan(app: FastAPI):
    global TEXTS, EMBEDDINGS

    print("Cargar textos iniciales...")
    TEXTS = load_texts()

    print("Generar embeddings iniciales...")
    EMBEDDINGS, _ = generate_all_embeddings(TEXTS)

    print("Motor semántico listo.")

    yield

    print("Cerrando microservicio...")

app = FastAPI(lifespan=lifespan)

class SearchQuery(BaseModel):
    query: str
    top_k: int = 3

class EmbedInput(BaseModel):
    text: str

class ProductInput(BaseModel):
    text: str

# ---- Endpoints ----
@app.post("/search")
def search_items(payload: SearchQuery):
    results = search(payload.query, EMBEDDINGS, TEXTS, payload.top_k)
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
    global EMBEDDINGS, TEXTS
    vec = generate_embeddings(payload.text)[0]
    EMBEDDINGS = np.vstack([EMBEDDINGS, vec.reshape(1, -1)])
    TEXTS.append(payload.text)

    return {
        "status": "ok",
        "product_added": payload.text
    }

@app.post("/suggest_category")
def suggest_category_endpoint(payload: ProductInput):
    result = suggest_category(payload.text)
    return result
