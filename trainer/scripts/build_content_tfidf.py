import pandas as pd
from pathlib import Path
from sklearn.feature_extraction.text import TfidfVectorizer
from scipy.sparse import save_npz

ROOT = Path(".")
OUT = ROOT / "artifacts"

CORPUS = OUT / "movie_corpus_genome.csv"
TFIDF_OUT = OUT / "content_tfidf.npz"
MOVIE_IDS_OUT = OUT / "content_movie_ids.csv"

def main():
    print("Reading semantic corpus...")
    df = pd.read_csv(CORPUS)

    # Safety
    df["text"] = df["text"].fillna("").astype(str)

    movie_ids = df["movieId"].astype(int)
    texts = df["text"].tolist()

    print("Running TF-IDF vectorization...")
    vectorizer = TfidfVectorizer(
        stop_words="english",
        ngram_range=(1, 2),
        min_df=2,
        max_features=100000
    )

    X = vectorizer.fit_transform(texts)

    print("\nDONE")
    print("Matrix shape:", X.shape)
    print("Vocabulary size:", len(vectorizer.vocabulary_))

    print("Saving sparse matrix...")
    save_npz(TFIDF_OUT, X)

    print("Saving movie id mapping...")
    movie_ids.to_frame(name="movieId").to_csv(MOVIE_IDS_OUT, index=False)

    print("Saved:", TFIDF_OUT)
    print("Saved:", MOVIE_IDS_OUT)

if __name__ == "__main__":
    main()