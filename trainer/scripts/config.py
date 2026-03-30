from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]

DATA_RAW = ROOT / "data" / "raw"

MOVIES_CSV  = DATA_RAW / "movies.csv"
RATINGS_CSV = DATA_RAW / "ratings.csv"
LINKS_CSV   = DATA_RAW / "links.csv"


DB_HOST = "localhost"
DB_PORT = 3306
DB_NAME = "moviereco"
DB_USER = "root"
DB_PASS = "NewPass123!"


SQLALCHEMY_URL = f"mysql+pymysql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"