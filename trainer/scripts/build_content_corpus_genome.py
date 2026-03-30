import pandas as pd
import re
from pathlib import Path

ROOT = Path(".")
RAW = ROOT / "data" / "raw"
OUT = ROOT / "artifacts"
OUT.mkdir(parents=True, exist_ok=True)

MOVIES = RAW / "movies.csv"
GENOME_TAGS = RAW / "genome-tags.csv"
GENOME_SCORES = RAW / "genome-scores.csv"

TOP_TAGS_PER_MOVIE = 15

def clean_title(title: str) -> str:
    title = str(title).lower().strip()
    title = re.sub(r"\(\d{4}\)\s*$", "", title)   # remove year at end
    title = re.sub(r"[^a-z0-9\s]", " ", title)    # remove punctuation
    title = re.sub(r"\s+", " ", title).strip()    # normalize spaces
    return title

def clean_genres(genres: str) -> str:
    genres = str(genres).lower().replace("|", " ")
    genres = re.sub(r"[^a-z0-9\s]", " ", genres)
    genres = re.sub(r"\s+", " ", genres).strip()
    return genres

def clean_tag(tag: str) -> str:
    tag = str(tag).lower().strip()
    tag = re.sub(r"[^a-z0-9\s]", " ", tag)
    tag = re.sub(r"\s+", " ", tag).strip()
    return tag

def main():
    print("Reading movies...")
    movies = pd.read_csv(MOVIES, usecols=["movieId", "title", "genres"])

    print("Reading genome tags...")
    genome_tags = pd.read_csv(GENOME_TAGS)   # columns: tagId, tag
    genome_tags["tag"] = genome_tags["tag"].apply(clean_tag)

    # tagId -> tag text
    tag_map = dict(zip(genome_tags["tagId"], genome_tags["tag"]))

    print("Reading genome scores...")
    scores = pd.read_csv(GENOME_SCORES)   # columns: movieId, tagId, relevance

    print("Selecting top genome tags per movie...")
    scores = scores.sort_values(["movieId", "relevance"], ascending=[True, False])
    top_scores = scores.groupby("movieId").head(TOP_TAGS_PER_MOVIE).copy()

    print("Mapping tag ids to tag text...")
    top_scores["tag"] = top_scores["tagId"].map(tag_map)

    print("Aggregating tag text per movie...")
    movie_tags = (
        top_scores.groupby("movieId")["tag"]
        .apply(lambda x: " ".join([t for t in x.dropna().tolist() if t]))
        .reset_index()
        .rename(columns={"tag": "genome_text"})
    )

    print("Cleaning movie metadata...")
    movies["title_text"] = movies["title"].apply(clean_title)
    movies["genre_text"] = movies["genres"].apply(clean_genres)

    print("Merging movie metadata with genome text...")
    corpus = movies.merge(movie_tags, on="movieId", how="left")
    corpus["genome_text"] = corpus["genome_text"].fillna("")

    print("Building final text field...")
    corpus["text"] = (
        corpus["title_text"] + " " +
        corpus["genre_text"] + " " +
        corpus["genome_text"]
    ).str.strip()

    corpus = corpus[["movieId", "title", "text"]]

    out_path = OUT / "movie_corpus_genome.csv"
    corpus.to_csv(out_path, index=False)

    print("\nDONE")
    print("Saved:", out_path)
    print("Rows:", len(corpus))
    print(corpus.head(3))

if __name__ == "__main__":
    main()