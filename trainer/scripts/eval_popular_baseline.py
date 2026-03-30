import pandas as pd
import numpy as np
import json
from pathlib import Path

ROOT = Path(".")
OUT = ROOT / "artifacts"

TRAIN = OUT / "eval_train_ratings.csv"
HELDOUT = OUT / "eval_heldout.csv"

TOP_K = 10

def main():
    print("Reading evaluation split...")
    train = pd.read_csv(TRAIN, usecols=["userId", "movieId", "rating"])
    heldout = pd.read_csv(HELDOUT, usecols=["userId", "movieId"])

    print("Building popularity list from train split...")
    grp = train.groupby("movieId")["rating"].agg(["count", "mean"]).reset_index()

    # Simple weighted popularity score:
    # popularityScore = meanRating * log1p(count)
    grp["score"] = grp["mean"] * np.log1p(grp["count"])

    grp = grp.sort_values("score", ascending=False)
    popular_topk = grp["movieId"].head(TOP_K).astype(int).tolist()

    print("Top popular@10:", popular_topk)

    print("Evaluating HitRate@10...")
    hits = 0
    recommended_unique = set(popular_topk)

    for row in heldout.itertuples(index=False):
        user_id = int(row.userId)
        true_movie = int(row.movieId)

        # same popular top-k for all users
        recs = popular_topk

        if true_movie in recs:
            hits += 1

    num_users = len(heldout)
    hit_rate_at_10 = hits / num_users if num_users > 0 else 0.0
    coverage_at_10 = len(recommended_unique)

    metrics = {
        "strategy": "popular",
        "num_users": int(num_users),
        "top_k": TOP_K,
        "hit_rate_at_10": float(hit_rate_at_10),
        "coverage_at_10": int(coverage_at_10),
        "popular_topk": popular_topk
    }

    out_path = OUT / "metrics_popular.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print("\nDONE")
    print("Users:", num_users)
    print("HitRate@10:", round(hit_rate_at_10, 4))
    print("Coverage@10:", coverage_at_10)
    print("Saved:", out_path)

if __name__ == "__main__":
    main()