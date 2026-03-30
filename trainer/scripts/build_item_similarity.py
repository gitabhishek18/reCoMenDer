import json
import numpy as np
import pandas as pd
from pathlib import Path
from scipy.sparse import load_npz, csr_matrix

OUT = Path("artifacts")
MATRIX_PATH = OUT / "ratings_matrix.npz"
MOVIE_INDEX = OUT / "movie_index.csv"
OUT_JSON = OUT / "similar_items_cf.json"

TOPK = 100          # neighbors per movie
BLOCK = 250         # number of movies per block; reduce if memory issues

def l2_normalize_rows(X):
    # Ensure CSR (sliceable)
    X = X.tocsr()
    row_norm = np.sqrt(X.multiply(X).sum(axis=1)).A1
    row_norm[row_norm == 0] = 1.0
    inv = 1.0 / row_norm
    # multiply may return COO -> force CSR
    return X.multiply(inv[:, None]).tocsr()

def main():
    # Load sparse matrix: shape = (n_users, n_movies)
    R = load_npz(MATRIX_PATH).tocsr()

    # We want item vectors, so take transpose: (n_movies, n_users)
    A = R.T.tocsr()

    # L2 normalize each movie vector -> cosine similarity = dot product
    A = l2_normalize_rows(A)

    # Map column index -> movieId
    movie_df = pd.read_csv(MOVIE_INDEX)  # columns: col, movieId
    movie_ids = movie_df["movieId"].astype(int).tolist()
    n_items = len(movie_ids)

    # Output structure: { "movieId": [{"movieId": other, "score": sim}, ...], ... }
    result = {}

    # Process blocks of rows (movies)
    for start in range(0, n_items, BLOCK):
        end = min(start + BLOCK, n_items)
        print(f"Block {start}..{end-1} / {n_items}")

        A_block = A[start:end]                    # (B, n_users)
        S = (A_block @ A.T).tocsr()              # (B, n_items) sparse similarities

        for bi in range(end - start):
            i = start + bi
            row = S.getrow(bi)

            # Get all non-zero similarities for this movie
            idx = row.indices
            sim = row.data

            # Remove self-similarity (i with i)
            mask = idx != i
            idx = idx[mask]
            sim = sim[mask]

            if sim.size == 0:
                result[str(movie_ids[i])] = []
                continue

            # Keep only topK
            if sim.size > TOPK:
                top_idx = np.argpartition(-sim, TOPK)[:TOPK]
                idx = idx[top_idx]
                sim = sim[top_idx]

            # Sort descending
            order = np.argsort(-sim)
            idx = idx[order]
            sim = sim[order]

            result[str(movie_ids[i])] = [
                {"movieId": int(movie_ids[j]), "score": float(s)}
                for j, s in zip(idx, sim)
            ]

    with open(OUT_JSON, "w", encoding="utf-8") as f:
        json.dump(result, f)

    print("\nDONE")
    print("Saved:", OUT_JSON)
    print("Movies with neighbors:", len(result))

if __name__ == "__main__":
    main()