import unicodedata
import re

def normalize_text(text: str) -> str:
    """
    Normaliza el texto para mejorar la calidad del embedding.
    """
    if not isinstance(text, str):
        raise ValueError("normalize_text: se esperaba un string")
    
    # 1. Minúsculas
    text = text.lower()

    # 2. Quitar acentos
    text = unicodedata.normalize('NFD', text)
    text = ''.join(c for c in text if unicodedata.category(c) != 'Mn')

    # 3. Mantener solo letras, números y espacios
    text = re.sub(r'[^a-z0-9\s]', ' ', text)

    # 4. Quitar espacios múltiples
    text = re.sub(r'\s+', ' ', text).strip()

    return text
