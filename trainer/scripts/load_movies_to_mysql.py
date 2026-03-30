import pandas as pd
from sqlalchemy import create_engine
from config import SQLALCHEMY_URL, MOVIES_CSV

def main():
    engine = create_engine(SQLALCHEMY_URL)

    movies = pd.read_csv(MOVIES_CSV)

    # Match your table columns:
    # If your SQL table column is `id`, rename movieId->id
    if "id" in [c.lower() for c in pd.read_sql("SHOW COLUMNS FROM movies", engine)["Field"]]:
        movies = movies.rename(columns={"movieId": "id"})
    # else assume table has movieId column

    # Add year if your table has 'year'
    cols = pd.read_sql("SHOW COLUMNS FROM movies", engine)["Field"].tolist()
    if "year" in cols:
        movies["year"] = movies["title"].str.extract(r"\((\d{4})\)\s*$")[0]
        movies["year"] = pd.to_numeric(movies["year"], errors="coerce")

    movies.to_sql("movies", engine, if_exists="append", index=False, chunksize=5000, method="multi")
    print("Inserted movies:", len(movies))

if __name__ == "__main__":
    main()