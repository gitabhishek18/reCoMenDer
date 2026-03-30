import pandas as pd
from pathlib import Path

RATINGS = Path("data/raw/ratings.csv")
OUT = Path("artifacts")
OUT.mkdir(parents=True, exist_ok=True)

CHUNK = 1_000_000
TOP_M = 10_000

def main():
    counts = {}
    processed = 0

    for chunk in pd.read_csv(RATINGS, usecols=["movieId"], chunksize=CHUNK):
        vc = chunk["movieId"].value_counts()
        for mid, c in vc.items():
            mid = int(mid)
            counts[mid] = counts.get(mid, 0) + int(c)

        processed += len(chunk)
        print("processed rows:", processed)

    top = sorted(counts.items(), key=lambda x: -x[1])[:TOP_M]
    pd.DataFrame(top, columns=["movieId", "count"]).to_csv(OUT / "top_movies.csv", index=False)
    print("Saved:", OUT / "top_movies.csv", "rows:", len(top))

if __name__ == "__main__":
    main()