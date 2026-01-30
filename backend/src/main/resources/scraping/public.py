
class ApiPublic:
    fotmobplayers: str = "https://www.fotmob.com/api/data/playerData?id="
    fotmobtable: str = "https://www.fotmob.com/api/data/tltable?leagueId=87"
    fotmoblaliga: str = "https://www.fotmob.com/api/data/leagues?id=87"
    xmaskey: str = ""
    headers: dict = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        "AppleWebKit/537.36 (KHTML, like Gecko) "
                        "Chrome/139.0.0.0 Safari/537.36",
        "X-Mas": xmaskey
    }
