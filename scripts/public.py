import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv(Path(__file__).resolve().parent / ".env")


class ApiPublic:
    BASE_URL: str = "https://v3.football.api-sports.io"
    API_KEY: str = os.environ.get("API_FOOTBALL_KEY", "")
    LEAGUE_ID: int = 140  
    SEASON: int = 2025

    headers: dict = {
        "x-apisports-key": API_KEY
    }

    RATE_LIMIT_DELAY: float = 0.5        
    RATE_LIMIT_DELAY_SQUADS: float = 0.5  

   
    TEAM_IDS: list = [
        541,   # Real Madrid
        529,   # Barcelona
        530,   # Atletico Madrid
        548,   # Real Sociedad
        543,   # Real Betis
        531,   # Athletic Bilbao
        533,   # Villarreal
        727,   # Osasuna
        547,   # Girona
        798,   # Mallorca
        538,   # Celta Vigo
        728,   # Rayo Vallecano
        546,   # Getafe
        536,   # Sevilla
        532,   # Valencia
        540,   # Espanyol
        542,   # Alaves
        718,   # Real Oviedo
        539,   # Levante
        797,   # Elche
    ]
