import pandas as pd
from pathlib import Path

ROOT = Path(".")
RAW = ROOT / "data" / "raw"
OUT = ROOT / "artifacts"
OUT.mkdir(parents=True, exist_ok=True)

RATINGS = RAW / "ratings.csv"

MIN_USER_RATINGS = 20
RELEVANT_THRESHOLD = 4.0
MAX_USERS = 5000   # keep evaluation manageable for now

def main():
    print("Reading ratings...")
    df = pd.read_csv(RATINGS, usecols=["userId", "movieId", "rating", "timestamp"])

    print("Filtering eligible users...")
    user_counts = df.groupby("userId").size()
    eligible_users = user_counts[user_counts >= MIN_USER_RATINGS].index

    df = df[df["userId"].isin(eligible_users)]

    print("Finding relevant ratings...")
    relevant = df[df["rating"] >= RELEVANT_THRESHOLD].copy()

    # most recent relevant rating per user
    relevant = relevant.sort_values(["userId", "timestamp"], ascending=[True, False])
    heldout = relevant.groupby("userId").head(1).copy()

    # keep only users where heldout exists
    heldout_users = heldout["userId"].unique()
    df = df[df["userId"].isin(heldout_users)]
    heldout = heldout[heldout["userId"].isin(heldout_users)]

    # optionally sample users
    heldout_users = sorted(heldout["userId"].unique())[:MAX_USERS]
    df = df[df["userId"].isin(heldout_users)]
    heldout = heldout[heldout["userId"].isin(heldout_users)]

    # remove heldout interaction from train
    train = df.merge(
        heldout[["userId", "movieId", "timestamp"]],
        on=["userId", "movieId", "timestamp"],
        how="left",
        indicator=True
    )
    train = train[train["_merge"] == "left_only"].drop(columns=["_merge"])

    train_out = OUT / "eval_train_ratings.csv"
    heldout_out = OUT / "eval_heldout.csv"

    train.to_csv(train_out, index=False)
    heldout.to_csv(heldout_out, index=False)

    print("DONE")
    print("Train rows:", len(train))
    print("Heldout rows:", len(heldout))
    print("Users:", len(heldout_users))
    print("Saved:", train_out)
    print("Saved:", heldout_out)

if __name__ == "__main__":
    main()