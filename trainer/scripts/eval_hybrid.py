import json
import pandas as pd
from pathlib import Path
from collections import defaultdict
import math

ROOT = Path(".")
OUT = ROOT / "artifacts"

TRAIN = OUT / "eval_train_ratings.csv"
HELDOUT = OUT / "eval_heldout.csv"
CF_JSON = OUT / "eval_similar_items_cf.json"
CONTENT_JSON = OUT / "similar_items_content.json"

TOP_K = 10

def preference_weight(rating: float) -> float:
    return (rating - 3.0) / 2.0

def compute_cf_user_confidence(num_ratings: int) -> float:
    if num_ratings < 5:
        return 0.30
    if num_ratings < 10:
        return 0.50
    if num_ratings < 20:
        return 0.70
    return 0.85

def normalize_scores(score_map):
    positives = {k: v for k, v in score_map.items() if v > 0.0}
    if not positives:
        return {}
    mx = max(positives.values())
    if mx <= 0.0:
        return {}
    return {k: v / mx for k, v in positives.items()}

def main():
    print("Reading eval split...")
    train = pd.read_csv(TRAIN, usecols=["userId", "movieId", "rating"])
    heldout = pd.read_csv(HELDOUT, usecols=["userId", "movieId"])

    print("Loading CF artifact...")
    with open(CF_JSON, "r", encoding="utf-8") as f:
        cf_sim = json.load(f)

    print("Loading content artifact...")
    with open(CONTENT_JSON, "r", encoding="utf-8") as f:
        content_sim = json.load(f)

    print("Building user histories...")
    user_history = defaultdict(list)
    seen_movies = defaultdict(set)

    for row in train.itertuples(index=False):
        u = int(row.userId)
        m = int(row.movieId)
        r = float(row.rating)
        user_history[u].append((m, r))
        seen_movies[u].add(m)

    print("Building popularity prior from train split...")
    grp = train.groupby("movieId")["rating"].agg(["count", "mean"]).reset_index()
    grp["score"] = grp["mean"] * grp["count"].apply(lambda x: math.log1p(x))
    pop_score_map = dict(zip(grp["movieId"].astype(int), grp["score"].astype(float)))
    max_pop = max(pop_score_map.values()) if pop_score_map else 1.0

    print("Evaluating hybrid HitRate@10...")
    hits = 0
    recommended_unique = set()

    for row in heldout.itertuples(index=False):
        u = int(row.userId)
        true_movie = int(row.movieId)

        history = user_history.get(u, [])
        seen = seen_movies.get(u, set())

        if not history:
            continue

        cf_scores = defaultdict(float)
        content_scores = defaultdict(float)

        for src_movie, rating in history:
            w = preference_weight(rating)
            if w == 0.0:
                continue

            for nb in cf_sim.get(str(src_movie), []):
                cand = int(nb["movieId"])
                sim = float(nb["score"])
                if cand in seen:
                    continue
                cf_scores[cand] += w * sim

            for nb in content_sim.get(str(src_movie), []):
                cand = int(nb["movieId"])
                sim = float(nb["score"])
                if cand in seen:
                    continue
                content_scores[cand] += w * sim

        cf_norm = normalize_scores(cf_scores)
        content_norm = normalize_scores(content_scores)

        if not cf_norm and not content_norm:
            continue

        cf_conf = compute_cf_user_confidence(len(history))
        content_conf = 1.0 - cf_conf

        all_candidates = set(cf_norm.keys()) | set(content_norm.keys())

        final_scores = {}
        for cand in all_candidates:
            cf_score = cf_norm.get(cand, 0.0)
            content_score = content_norm.get(cand, 0.0)
            pop_score = pop_score_map.get(cand, 0.0) / max_pop if max_pop > 0 else 0.0

            personalized = (cf_conf * cf_score) + (content_conf * content_score)
            final_score = (0.85 * personalized) + (0.15 * pop_score)

            if final_score > 0.0:
                final_scores[cand] = final_score

        ranked = sorted(final_scores.items(), key=lambda x: -x[1])
        recs = [mid for mid, _ in ranked[:TOP_K]]
        recommended_unique.update(recs)

        if true_movie in recs:
            hits += 1

    num_users = len(heldout)
    hit_rate_at_10 = hits / num_users if num_users > 0 else 0.0
    coverage_at_10 = len(recommended_unique)

    metrics = {
        "strategy": "hybrid",
        "num_users": int(num_users),
        "top_k": TOP_K,
        "hit_rate_at_10": float(hit_rate_at_10),
        "coverage_at_10": int(coverage_at_10)
    }

    out_path = OUT / "metrics_hybrid.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print("\nDONE")
    print("Users:", num_users)
    print("HitRate@10:", round(hit_rate_at_10, 4))
    print("Coverage@10:", coverage_at_10)
    print("Saved:", out_path)

if __name__ == "__main__":
    main()