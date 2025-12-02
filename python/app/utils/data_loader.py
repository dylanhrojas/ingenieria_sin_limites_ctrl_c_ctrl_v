import pandas as pd
from typing import List

def load_texts_from_csv(file_path: str, col_name: str) -> List[str]:
    """
    Carga la columna deseada de los productos.
    Retorna la lista de strings.
    """
    df = pd.read_csv(file_path, encoding="latin-1")
    return df[col_name].dropna().drop_duplicates().astype(str).tolist()