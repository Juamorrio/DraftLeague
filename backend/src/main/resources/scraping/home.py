import json
import os
import sys
import requests



def fetch_laliga_season(league_id=87):

    url = f"https://www.fotmob.com/api/data/leagues?id={league_id}"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "application/json,text/plain,*/*",
        "Referer": "https://www.fotmob.com/",
    }
    
    try:
        resp = requests.get(url, headers=headers, timeout=30)
        resp.raise_for_status()
        data = resp.json()
    except requests.RequestException as e:
        print(f"Error descargando datos: {e}")
        return None
    
    all_matches = data.get("fixtures", {}).get("allMatches", [])
    
    print(f"Se encontraron {len(all_matches)} partidos totales.")
    return all_matches


def parse_score(score_str):
    """
    Parsea un string como '1 - 0' o '2 - 2' en (homeScore, awayScore).
    Devuelve (None, None) si no se puede parsear.
    """
    if not score_str:
        return None, None
    parts = score_str.strip().split("-")
    if len(parts) != 2:
        return None, None
    try:
        home_score = int(parts[0].strip())
        away_score = int(parts[1].strip())
        return home_score, away_score
    except ValueError:
        return None, None


def build_matches_json(all_matches):
    
    matches_by_round = {}
    
    for match in all_matches:
        status = match.get("status", {})
        finished = status.get("finished", False)
        
        # Solo incluir partidos finalizados
        if not finished:
            continue
        
        round_id = match.get("round") or match.get("roundName")
        home_id = match.get("home", {}).get("id")
        away_id = match.get("away", {}).get("id")
        score_str = status.get("scoreStr", "")
        home_score, away_score = parse_score(score_str)
        jornada_key = f"jornada_{round_id}"
        if jornada_key not in matches_by_round:
            matches_by_round[jornada_key] = []
        
        matches_by_round[jornada_key].append({
            "homeTeamId": int(home_id),
            "awayTeamId": int(away_id),
            "homeScore": home_score,
            "awayScore": away_score
        })
    
    return matches_by_round


def build_upcoming_matches_json(all_matches):

    upcoming_by_round = {}
    
    for match in all_matches:
        status = match.get("status", {})
        finished = status.get("finished", False)
        not_started = match.get("notStarted", False)
        
        if finished:
            continue
        
        round_id = match.get("round") or match.get("roundName")
        home_id = match.get("home", {}).get("id")
        away_id = match.get("away", {}).get("id")   
        match_date = status.get("utcTime") or match.get("status", {}).get("utcTime")
        
        jornada_key = f"jornada_{round_id}"
        if jornada_key not in upcoming_by_round:
            upcoming_by_round[jornada_key] = []
        
        match_data = {
            "homeTeamId": int(home_id),
            "awayTeamId": int(away_id)
        }
        if match_date:
            match_data["matchDate"] = match_date
        
        upcoming_by_round[jornada_key].append(match_data)
    
    return upcoming_by_round


def main():

    all_matches = fetch_laliga_season(league_id=87)
    if all_matches is None:
        print("No se pudo obtener la lista de partidos.")
        return 1
    
    matches_json = build_matches_json(all_matches)
    if not matches_json:
        print("No se generaron datos de partidos finalizados.")
    else:
        total_rounds = len(matches_json)
        total_matches = sum(len(v) for v in matches_json.values())
        print(f"Generados {total_matches} partidos finalizados en {total_rounds} jornadas.")
        
        output_path = os.path.join(os.path.dirname(__file__), "matches.json")
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(matches_json, f, indent=2, ensure_ascii=False)
        print(f"Partidos finalizados guardados en: {output_path}")
    
    upcoming_json = build_upcoming_matches_json(all_matches)
    if not upcoming_json:
        print("No se generaron datos de partidos próximos.")
    else:
        total_rounds_upcoming = len(upcoming_json)
        total_upcoming = sum(len(v) for v in upcoming_json.values())
        print(f"Generados {total_upcoming} partidos próximos en {total_rounds_upcoming} jornadas.")
        
        upcoming_path = os.path.join(os.path.dirname(__file__), "upcoming_matches.json")
        with open(upcoming_path, "w", encoding="utf-8") as f:
            json.dump(upcoming_json, f, indent=2, ensure_ascii=False)
        print(f"Partidos próximos guardados en: {upcoming_path}")
    
    print("Completado con éxito!")
    return 0


if __name__ == "__main__":
    sys.exit(main())
