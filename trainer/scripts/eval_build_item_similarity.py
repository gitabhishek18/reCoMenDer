import json
import numpy as np
import pandas as pd
from pathlib import Path
from scipy.sparse import load_npz, csr_matrix

ROOT = Path(".")
OUT = ROOT / "artifacts"

MATRIX_PATH = OUT / "eval_ratings_matrix.npz"
MOVIE_INDEX = OUT / "eval_movie_index.csv"
OUT_JSON = OUT / "eval_similar_items_cf.json"

TOPK = 100
BLOCK = 250

def l2_normalize_rows(X: csr_matrix) -> csr_matrix:
    X = X.tocsr()
    row_norm = np.sqrt(X.multiply(X).sum(axis=1)).A1
    row_norm[row_norm == 0] = 1.0
    inv = 1.0 / row_norm
    return X.multiply(inv[:, None]).tocsr()

def main():
    print("Loading matrix...")
    R = load_npz(MATRIX_PATH).tocsr()
    A = R.T.tocsr()
    A = l2_normalize_rows(A).tocsr()

    movie_ids = pd.read_csv(MOVIE_INDEX)["movieId"].astype(int).tolist()
    n_items = len(movie_ids)

    result = {}

    for start in range(0, n_items, BLOCK):
        end = min(start + BLOCK, n_items)
        print(f"Block {start}..{end-1} / {n_items}")

        A_block = A[start:end]
        S = (A_block @ A.T).tocsr()

        for bi in range(end - start):
            i = start + bi
            row = S.getrow(bi)

            idx = row.indices
            sim = row.data

            mask = idx != i
            idx = idx[mask]
            sim = sim[mask]

            if sim.size == 0:
                result[str(movie_ids[i])] = []
                continue

            if sim.size > TOPK:
                top_idx = np.argpartition(-sim, TOPK - 1)[:TOPK]
                idx = idx[top_idx]
                sim = sim[top_idx]

            order = np.argsort(-sim)
            idx = idx[order]
            sim = sim[order]

            result[str(movie_ids[i])] = [
                {"movieId": int(movie_ids[j]), "score": round(float(s), 4)}
                for j, s in zip(idx, sim)
            ]

    with open(OUT_JSON, "w", encoding="utf-8") as f:
        json.dump(result, f)

    print("DONE")
    print("Saved:", OUT_JSON)
    print("Movies with neighbors:", len(result))

if __name__ == "__main__":
    main()