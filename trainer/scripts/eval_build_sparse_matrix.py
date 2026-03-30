import pandas as pd
import numpy as np
from scipy.sparse import csr_matrix, save_npz
from pathlib import Path

ROOT = Path(".")
OUT = ROOT / "artifacts"

TRAIN = OUT / "eval_train_ratings.csv"
TOP_MOVIES = OUT / "eval_top_movies.csv"

MATRIX_OUT = OUT / "eval_ratings_matrix.npz"
USER_INDEX_OUT = OUT / "eval_user_index.csv"
MOVIE_INDEX_OUT = OUT / "eval_movie_index.csv"

def main():
    print("Reading top movies...")
    top = pd.read_csv(TOP_MOVIES)
    top_movies = top["movieId"].astype(int).tolist()
    movie_to_col = {mid: idx for idx, mid in enumerate(top_movies)}

    print("Reading eval train ratings...")
    df = pd.read_csv(TRAIN, usecols=["userId", "movieId", "rating"])
    df = df[df["movieId"].isin(movie_to_col.keys())]

    user_ids = sorted(df["userId"].astype(int).unique().tolist())
    user_to_row = {uid: idx for idx, uid in enumerate(user_ids)}

    rows = df["userId"].map(user_to_row).astype(np.int32).to_numpy()
    cols = df["movieId"].map(movie_to_col).astype(np.int32).to_numpy()
    vals = df["rating"].astype(np.float32).to_numpy()

    mat = csr_matrix(
        (vals, (rows, cols)),
        shape=(len(user_ids), len(top_movies))
    )

    save_npz(MATRIX_OUT, mat)

    pd.DataFrame({"row": range(len(user_ids)), "userId": user_ids}).to_csv(USER_INDEX_OUT, index=False)
    pd.DataFrame({"col": range(len(top_movies)), "movieId": top_movies}).to_csv(MOVIE_INDEX_OUT, index=False)

    print("DONE")
    print("Matrix shape:", mat.shape)
    print("Non-zeros:", mat.nnz)
    print("Saved:", MATRIX_OUT)
    print("Saved:", USER_INDEX_OUT)
    print("Saved:", MOVIE_INDEX_OUT)

if __name__ == "__main__":
    main()