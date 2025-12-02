import numpy as np

from services.embeddings import generate_embeddings, cosine_similarity
from utils.text_cleaner import normalize_text
from typing import List, Tuple, Dict, Any
from numpy.typing import NDArray

CATEGORIES = ["agua", "botana", "lácteos", "bebidas energéticas", "electrónica", "hogar"]
CATEGORY_EMBS = generate_embeddings(CATEGORIES)

# Función para obtener textos (items)
def load_texts() -> List[str]:
    raw_texts = [
        "Laptop Lenovo ThinkPad X1 Carbon con procesador i7 y 16GB RAM",
        "Smartphone Samsung Galaxy con pantalla AMOLED",
        "Mouse inalámbrico Logitech MX Master 3",
        "Monitor Dell 27 pulgadas QHD",
        "Teclado mecánico RGB para gaming",
        "Audífonos Bluetooth con cancelación de ruido",
        "Cámara digital DSLR profesional",
        "Impresora multifuncional HP con Wi-Fi",
        "Disco duro externo de 2TB USB 3.0",
        "Tablet Apple iPad con pantalla retina"
    ]

    return [normalize_text(t) for t in raw_texts]

# Función para generar embeddings según load_texts()
def generate_all_embeddings(texts: List[str]) -> Tuple[NDArray[np.float32], List[str]]:
    """
    Retorna embeddings (lista de vectores) y la lista de textos original
    """
    cleaned_texts = [normalize_text(t) for t in texts]

    embeddings = generate_embeddings(cleaned_texts)
    return embeddings, texts

def search(query: str, embeddings: NDArray[np.float32], texts: List[str], top_k: int = 3) -> List[Dict[str, Any]]:
    query_clean = normalize_text(query)
    q_emb = generate_embeddings(query_clean)[0]

    scores = [cosine_similarity(q_emb, e) for e in embeddings]
    top_idx = sorted(range(len(scores)), key = lambda i : scores[i], reverse=True)[:top_k]

    results = []
    for rank, i in enumerate(top_idx, start=1):
        results.append({
            "text": texts[i],
            "score": scores[i],
            "rank": rank
        })

    return results

def suggest_category(product_text: str):
    product_emb = generate_embeddings(product_text)[0]

    scores = [cosine_similarity(product_emb, c_emb) for c_emb in CATEGORY_EMBS]
    
    best_idx = scores.index(max(scores))
    
    return {
        "product": product_text,
        "category": CATEGORIES[best_idx],
        "score": scores[best_idx]
    }
