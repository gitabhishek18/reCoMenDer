import pandas as pd
from pathlib import Path

RAW = Path("data/raw")

def info(name):
    p = RAW / name
    df = pd.read_csv(p)
    print("\n==", name, "==")
    print("shape:", df.shape)
    print("cols :", list(df.columns)[:20])
    print(df.head(2))

info("movies.csv")
info("ratings.csv")
info("links.csv")
info("tags.csv")
info("genome-tags.csv")

print("\n== genome-scores.csv ==")
gs = pd.read_csv(RAW/"genome-scores.csv", nrows=5)
print("cols:", list(gs.columns))
print(gs)