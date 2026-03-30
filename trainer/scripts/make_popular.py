import pandas as pd
import numpy as np
import json
from pathlib import Path

RATINGS = Path("data/raw/ratings.csv")
MOVIES = Path("data/raw/movies.csv")
OUT = Path("artifacts")
OUT.mkdir(parents=True, exist_ok=True)

CHUNK = 1_000_000
TOP_N = 500

def main(vote_quantile=0.90):
    

    count = {}  
    ssum = {}   
    total_sum = 0.0
    total_cnt = 0

    processed = 0

    for chunk in pd.read_csv(RATINGS, usecols=["movieId", "rating"], chunksize=CHUNK):
        
        grp = chunk.groupby("movieId")["rating"].agg(["count", "sum"])

        
        for mid, row in grp.iterrows():
            mid = int(mid)
            c = int(row["count"])
            s = float(row["sum"])
            count[mid] = count.get(mid, 0) + c
            ssum[mid] = ssum.get(mid, 0.0) + s

        
        total_sum += float(chunk["rating"].sum())
        total_cnt += int(chunk.shape[0])

        processed += int(chunk.shape[0])
        print("processed rows:", processed)

    
    C = total_sum / total_cnt

    
    v_all = np.array(list(count.values()), dtype=np.float64)

    
    m = np.quantile(v_all, vote_quantile)

    
    movies = pd.read_csv(MOVIES, usecols=["movieId", "title"])
    title_map = dict(zip(movies["movieId"].astype(int), movies["title"].astype(str)))

    
    items = []
    for mid, v in count.items():
        R = ssum[mid] / v
        
        wr = (v / (v + m)) * R + (m / (v + m)) * C
        items.append((mid, wr, R, v))

    
    items.sort(key=lambda x: -x[1])
    top = items[:TOP_N]

    result = []
    for mid, wr, avg, v in top:
        result.append({
            "movieId": int(mid),
            "title": title_map.get(int(mid), ""),
            "score": float(wr),
            "avgRating": float(avg),
            "ratingCount": int(v),
            "reason": "Popular (weighted by rating count)"
        })

    out_path = OUT / "popular.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print("\nDONE")
    print("Global mean C =", C)
    print("Vote threshold m =", m)
    print("Saved:", out_path, "items:", len(result))

if __name__ == "__main__":
    main(vote_quantile=0.90)