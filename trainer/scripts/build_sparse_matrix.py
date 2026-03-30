import pandas as pd
import numpy as np
from scipy.sparse import csr_matrix, save_npz
from pathlib import Path

RATINGS = Path("data/raw/ratings.csv")
TOP_MOVIES = Path("artifacts/top_movies.csv")
OUT = Path("artifacts")
OUT.mkdir(parents=True, exist_ok=True)

CHUNK = 1_000_000

def main():
    top = pd.read_csv(TOP_MOVIES)
    top_movies = top["movieId"].astype(int).tolist()
    movie_to_col = {mid: idx for idx, mid in enumerate(top_movies)}

    user_to_row = {}
    rows, cols, vals = [], [], []

    processed = 0
    kept = 0

    for chunk in pd.read_csv(RATINGS, usecols=["userId", "movieId", "rating"], chunksize=CHUNK):
        
        chunk = chunk[chunk["movieId"].isin(movie_to_col.keys())]

        for u, m, r in chunk.itertuples(index=False):
            u = int(u); m = int(m); r = float(r)

            if u not in user_to_row:
                user_to_row[u] = len(user_to_row)

            rows.append(user_to_row[u])
            cols.append(movie_to_col[m])
            vals.append(r)

        processed += len(chunk)
        kept += len(chunk)
        print("processed chunk rows (kept):", len(chunk), "total kept:", kept)

    n_users = len(user_to_row)
    n_movies = len(top_movies)

    mat = csr_matrix((np.array(vals, dtype=np.float32),
                      (np.array(rows, dtype=np.int32), np.array(cols, dtype=np.int32))),
                     shape=(n_users, n_movies))

    save_npz(OUT / "ratings_matrix.npz", mat)

    
    user_index = pd.DataFrame({"row": list(user_to_row.values()), "userId": list(user_to_row.keys())})
    user_index.sort_values("row").to_csv(OUT / "user_index.csv", index=False)

    movie_index = pd.DataFrame({"col": list(range(n_movies)), "movieId": top_movies})
    movie_index.to_csv(OUT / "movie_index.csv", index=False)

    print("\nDONE")
    print("users:", n_users, "movies:", n_movies)
    print("non-zeros:", mat.nnz)
    print("saved:", OUT / "ratings_matrix.npz")

if __name__ == "__main__":
    main()