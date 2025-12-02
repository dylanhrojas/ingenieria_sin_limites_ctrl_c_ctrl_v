import numpy as np
import pickle 
import os 
from datetime import datetime

from services.embeddings import generate_embeddings, cosine_similarity
from utils.text_cleaner import normalize_text
from utils.data_loader import load_texts_from_csv, load_texts_with_categories_from_csv
from typing import List, Tuple, Dict, Any
from numpy.typing import NDArray

CATEGORIES = ["agua", "botana", "lácteos", "bebidas energéticas", "electrónica", "hogar"]
CATEGORY_EMBS = generate_embeddings(CATEGORIES)

# Función para obtener textos (items) con categorías
def load_texts() -> Tuple[List[str], List[str]]:
    """
    Carga textos de productos desde CSVs con sus categorías.
    Retorna tupla (textos_con_categoria, categorias)
    """
    # Cargar desde CSV 1 con categorías
    data1 = load_texts_with_categories_from_csv("data/convertcsv.csv", "full_description", "type")

    # Cargar desde CSV 2 limpio (ahora tiene columna 'type')
    data2 = load_texts_with_categories_from_csv("data/DetalleFacturas_clean.csv", "vcDescripcion", "type")

    # Combinar y eliminar duplicados
    all_data = data1 + data2

    # Usar dict para eliminar duplicados por nombre manteniendo categoría
    unique_data = {}
    for text, category in all_data:
        if text not in unique_data:
            unique_data[text] = category

    # Preparar textos enriquecidos con categoría
    enriched_texts = []
    categories = []

    for text, category in unique_data.items():
        # Combinar nombre + categoría para mejor embedding
        if category:
            enriched_text = f"{text} {category}"
        else:
            enriched_text = text

        enriched_texts.append(normalize_text(enriched_text))
        categories.append(category)

    return enriched_texts, categories

# Función para generar embeddings según load_texts()
def generate_all_embeddings(texts: List[str]) -> Tuple[NDArray[np.float32], List[str]]:
    """
    Retorna embeddings (lista de vectores) y la lista de textos original
    """
    cleaned_texts = [normalize_text(t) for t in texts]

    embeddings = generate_embeddings(cleaned_texts)
    return embeddings, texts

# Funciones de persistencia
def save_embeddings(embeddings: NDArray[np.float32], metadata: Dict, filepath: str = "data/embeddings") -> Dict[str, Any]:
    """
    Guarda embeddings y metadata a disco.
    """
    try:
        directory = os.path.dirname(filepath)
        if directory and not os.path.exists(directory):
            os.makedirs(directory)
        embeddings_file = filepath + ".npy"
        if os.path.exists(embeddings_file):
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup_file = filepath + f"_backup_{timestamp}.npy"
            os.rename(embeddings_file, backup_file)

        metadata["timestamp"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        metadata["count"] = len(embeddings)
        metadata["dimension"] = embeddings.shape[1] if len(embeddings) > 0 else 0

        np.save(embeddings_file, embeddings)

        metadata_file = filepath + "_metadata.pkl"
        with open(metadata_file, 'wb') as f:
            pickle.dump(metadata, f)

        size_bytes = os.path.getsize(embeddings_file)
        size_kb = size_bytes / 1024

        return {
            "success": True,
            "message": f"Guardados {len(embeddings)} embeddings exitosamente",
            "filepath": embeddings_file,
            "size_kb": round(size_kb, 2)
        }
    
    except Exception as e:
        return {
            "success": False,
            "message": f"Error al guardar embeddings: {str(e)}",
            "filepath": None,
            "size_kb": 0
        } 

def load_embeddings(filepath: str = "data/embeddings") -> Dict[str, Any]:
    """
    Carga embeddings y metadata desde disco.
    """
    try:
        embeddings_file = filepath + ".npy"
        metadata_file = filepath + "_metadata.pkl"

        if not os.path.exists(embeddings_file):
            return {
                "success": False,
                "embeddings": None,
                "metadata": None,
                "message": "No se encontraron embeddings guardados"
            }
        
        if not os.path.exists(metadata_file):
             return {
                "success": False,
                "embeddings": None,
                "metadata": None,
                "message": "No se encontró archivo de metadata"
            }
        
        embeddings = np.load(embeddings_file)

        with open(metadata_file, 'rb') as f:
            metadata = pickle.load(f)
        
        if len(embeddings) == 0:
            return {
                "success": False,
                "embeddings": None,
                "metadata": None,
                "message": "Archivo de embeddings está vacío"
            }
    
        if embeddings.shape[1] != 384:
            return {
                "success": False,
                "embeddings": None,
                "metadata": None,
                "message": f"Dimensión incorrecta: esperado 384, encontrado {embeddings.shape[1]}"
            }
        
        if "texts" not in metadata or "ids" not in metadata:
            return {
                "success": False,
                "embeddings": None,
                "metadata": None,
                "message": "Metadata incompleto: faltan claves 'texts' o 'ids'"
            }
        
        if len(embeddings) != len(metadata["texts"]):
            return {
                "success": False,
                "embeddings": None,
                "metadata": None,
                "message": f"Inconsistencia: {len(embeddings)} embeddings vs {len(metadata['texts'])} textos"
            }
        
        if len(embeddings) != len(metadata["ids"]):
            return {
                "success": False,
                "embeddings": None,
                "metadata": None,
                "message": f"Inconsistencia: {len(embeddings)} embeddings vs {len(metadata['ids'])} ids"
            }
        
        timestamp = metadata.get("timestamp", "desconocido")
        return {
            "success": True,
            "embeddings": embeddings,
            "metadata": metadata,
            "message": f"Cargados {len(embeddings)} embeddings desde {timestamp}"
        }
    
    except pickle.UnpicklingError:
        return {
            "success": False,
            "embeddings": None,
            "metadata": None,
            "message": "Error: archivo de metadata corrupto"
        }
    
    except Exception as e:
        return {
            "success": False,
            "embeddings": None,
            "metadata": None,
            "message": f"Error al cargar embeddings: {str(e)}"
        }

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
