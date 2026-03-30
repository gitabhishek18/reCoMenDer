import json
import pandas as pd
from pathlib import Path
from collections import defaultdict

ROOT = Path(".")
OUT = ROOT / "artifacts"

TRAIN = OUT / "eval_train_ratings.csv"
HELDOUT = OUT / "eval_heldout.csv"
CF_JSON = OUT / "eval_similar_items_cf.json"

TOP_K = 10

def preference_weight(rating: float) -> float:
    # same logic as backend
    return (rating - 3.0) / 2.0

def main():
    print("Reading eval split...")
    train = pd.read_csv(TRAIN, usecols=["userId", "movieId", "rating"])
    heldout = pd.read_csv(HELDOUT, usecols=["userId", "movieId"])

    print("Loading CF artifact...")
    with open(CF_JSON, "r", encoding="utf-8") as f:
        cf_sim = json.load(f)

    print("Building user histories...")
    user_history = defaultdict(list)
    seen_movies = defaultdict(set)

    for row in train.itertuples(index=False):
        u = int(row.userId)
        m = int(row.movieId)
        r = float(row.rating)
        user_history[u].append((m, r))
        seen_movies[u].add(m)

    print("Evaluating CF HitRate@10...")
    hits = 0
    recommended_unique = set()

    for row in heldout.itertuples(index=False):
        u = int(row.userId)
        true_movie = int(row.movieId)

        history = user_history.get(u, [])
        seen = seen_movies.get(u, set())

        score_map = defaultdict(float)

        for src_movie, rating in history:
            w = preference_weight(rating)
            if w == 0.0:
                continue

            neighbors = cf_sim.get(str(src_movie), [])
            for nb in neighbors:
                cand = int(nb["movieId"])
                sim = float(nb["score"])
                if cand in seen:
                    continue
                score_map[cand] += w * sim

        ranked = [(mid, score) for mid, score in score_map.items() if score > 0.0]
        ranked.sort(key=lambda x: -x[1])

        recs = [mid for mid, _ in ranked[:TOP_K]]
        recommended_unique.update(recs)

        if true_movie in recs:
            hits += 1

    num_users = len(heldout)
    hit_rate_at_10 = hits / num_users if num_users > 0 else 0.0
    coverage_at_10 = len(recommended_unique)

    metrics = {
        "strategy": "cf",
        "num_users": int(num_users),
        "top_k": TOP_K,
        "hit_rate_at_10": float(hit_rate_at_10),
        "coverage_at_10": int(coverage_at_10)
    }

    out_path = OUT / "metrics_cf.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print("\nDONE")
    print("Users:", num_users)
    print("HitRate@10:", round(hit_rate_at_10, 4))
    print("Coverage@10:", coverage_at_10)
    print("Saved:", out_path)

if __name__ == "__main__":
    main()