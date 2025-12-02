import pandas as pd
from typing import List, Tuple

def load_texts_from_csv(file_path: str, col_name: str) -> List[str]:
    """
    Carga la columna deseada de los productos.
    Retorna la lista de strings.
    """
    df = pd.read_csv(file_path, encoding="latin-1")
    return df[col_name].dropna().drop_duplicates().astype(str).tolist()

def load_texts_with_categories_from_csv(file_path: str, text_col: str, category_col: str = "type") -> List[Tuple[str, str]]:
    """
    Carga productos con sus categorías desde CSV.
    Retorna lista de tuplas (texto, categoría).
    """
    df = pd.read_csv(file_path, encoding="latin-1")

    # Filtrar filas con texto válido
    df = df[df[text_col].notna()]

    results = []
    for _, row in df.iterrows():
        text = str(row[text_col]).strip()
        category = str(row.get(category_col, "")).strip() if category_col in df.columns else ""

        if text:  # Solo agregar si el texto no está vacío
            results.append((text, category))

    return results