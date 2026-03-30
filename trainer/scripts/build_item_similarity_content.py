import json
import numpy as np
import pandas as pd
from pathlib import Path
from scipy.sparse import load_npz, csr_matrix

ROOT = Path(".")
OUT = ROOT / "artifacts"

TFIDF_PATH = OUT / "content_tfidf.npz"
MOVIE_IDS_PATH = OUT / "content_movie_ids.csv"
OUT_JSON = OUT / "similar_items_content.json"

TOPK = 30
BLOCK = 250   # reduce to 150 if memory gets tight
MIN_SIM=0.15

def l2_normalize_rows(X: csr_matrix) -> csr_matrix:
    X = X.tocsr()
    row_norm = np.sqrt(X.multiply(X).sum(axis=1)).A1
    row_norm[row_norm == 0] = 1.0
    inv = 1.0 / row_norm
    return X.multiply(inv[:, None]).tocsr()

def main():
    print("Loading TF-IDF matrix...")
    X = load_npz(TFIDF_PATH).tocsr()

    print("Loading movie id mapping...")
    movie_ids = pd.read_csv(MOVIE_IDS_PATH)["movieId"].astype(int).tolist()

    n_items = len(movie_ids)
    print("Movies:", n_items)
    print("Matrix shape:", X.shape)

    print("Normalizing rows for cosine similarity...")
    X = l2_normalize_rows(X)

    result = {}

    for start in range(0, n_items, BLOCK):
        end = min(start + BLOCK, n_items)
        print(f"Processing block {start}..{end-1} / {n_items}")

        X_block = X[start:end]           # shape: (block_size, vocab)
        S = (X_block @ X.T).tocsr()      # shape: (block_size, n_items)

        for bi in range(end - start):
            i = start + bi
            row = S.getrow(bi)

            idx = row.indices
            sim = row.data

            # remove self similarity
            mask = idx != i
            idx = idx[mask]
            sim = sim[mask]

            mask=sim>=MIN_SIM
            idx=idx[mask]
            sim=sim[mask]
            if sim.size == 0:
                result[str(movie_ids[i])] = []
                continue

            # keep only top K
            if sim.size > TOPK:
                top_idx = np.argpartition(-sim, TOPK)[:TOPK]
                idx = idx[top_idx]
                sim = sim[top_idx]

            # sort descending
            order = np.argsort(-sim)
            idx = idx[order]
            sim = sim[order]

            result[str(movie_ids[i])] = [
                {"movieId": int(movie_ids[j]), "score": round(float(s),4)}
                for j, s in zip(idx, sim)
            ]

    print("Saving JSON artifact...")
    with open(OUT_JSON, "w", encoding="utf-8") as f:
        json.dump(result, f)

    print("\nDONE")
    print("Saved:", OUT_JSON)
    print("Movies with neighbors:", len(result))

if __name__ == "__main__":
    main()