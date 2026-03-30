import pandas as pd
from pathlib import Path

ROOT = Path(".")
OUT = ROOT / "artifacts"

TRAIN = OUT / "eval_train_ratings.csv"
TOP_OUT = OUT / "eval_top_movies.csv"

TOP_M = 10000

def main():
    print("Reading eval train ratings...")
    df = pd.read_csv(TRAIN, usecols=["movieId"])

    counts = df["movieId"].value_counts().head(TOP_M)
    top = counts.reset_index()
    top.columns = ["movieId", "count"]

    top.to_csv(TOP_OUT, index=False)

    print("DONE")
    print("Saved:", TOP_OUT)
    print("Rows:", len(top))

if __name__ == "__main__":
    main()