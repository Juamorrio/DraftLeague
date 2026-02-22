import json
import os
import sys
import re
import time
import requests
from public import ApiPublic


def fetch_laliga_fixtures():
    """
    Fetch all La Liga fixtures for the season from API-Football.
    Uses 1 API request.
    """
    url = f"{ApiPublic.BASE_URL}/fixtures"
    params = {
        "league": ApiPublic.LEAGUE_ID,
        "season": ApiPublic.SEASON
    }

    try:
        resp = requests.get(url, headers=ApiPublic.headers, params=params, timeout=30)
        resp.raise_for_status()
        data = resp.json()
    except requests.RequestException as e:
        print(f"Error descargando fixtures: {e}")
        return None

    fixtures = data.get("response", [])
    print(f"Se encontraron {len(fixtures)} fixtures totales.")
    return fixtures


def fetch_fixture_xg(fixture_id):
    """
    Fetch expected goals (xG) for a fixture from /fixtures/statistics.
    Returns (home_xg, away_xg) or (None, None) on error.
    """
    url = f"{ApiPublic.BASE_URL}/fixtures/statistics"
    params = {"fixture": fixture_id}

    try:
        resp = requests.get(url, headers=ApiPublic.headers, params=params, timeout=30)
        resp.raise_for_status()
        data = resp.json()
    except requests.RequestException as e:
        print(f"  Error obteniendo xG para fixture {fixture_id}: {e}")
        return None, None

    teams = data.get("response", [])
    if len(teams) < 2:
        return None, None

    home_xg = None
    away_xg = None

    for i, team_data in enumerate(teams):
        stats = team_data.get("statistics", [])
        for stat in stats:
            if stat.get("type") == "expected_goals":
                val = stat.get("value")
                if val is not None:
                    try:
                        xg = float(val)
                    except (ValueError, TypeError):
                        xg = None
                    if i == 0:
                        home_xg = xg
                    else:
                        away_xg = xg
                break

    return home_xg, away_xg


def parse_round_number(round_str):
    """
    Parse round number from API-Football league.round string.
    Example: "Regular Season - 15" -> 15
    """
    if not round_str:
        return None
    match = re.search(r"(\d+)$", round_str)
    return int(match.group(1)) if match else None


def build_matches_json(fixtures):
    """
    Build played matches JSON from API-Football fixtures.
    Filters for finished matches (status.short == "FT").
    """
    matches_by_round = {}

    for fixture in fixtures:
        fixture_info = fixture.get("fixture", {})
        status = fixture_info.get("status", {})

        if status.get("short") != "FT":
            continue

        league = fixture.get("league", {})
        round_num = parse_round_number(league.get("round", ""))
        if round_num is None:
            continue

        fixture_id = fixture_info.get("id")
        teams = fixture.get("teams", {})
        goals = fixture.get("goals", {})

        home_team = teams.get("home", {})
        away_team = teams.get("away", {})

        jornada_key = f"jornada_{round_num}"
        if jornada_key not in matches_by_round:
            matches_by_round[jornada_key] = []

        matches_by_round[jornada_key].append({
            "fixtureId": fixture_id,
            "homeTeamId": home_team.get("id"),
            "awayTeamId": away_team.get("id"),
            "homeClub": home_team.get("name", ""),
            "awayClub": away_team.get("name", ""),
            "homeScore": goals.get("home"),
            "awayScore": goals.get("away"),
            "homeXg": None,
            "awayXg": None
        })

    return matches_by_round


def enrich_matches_with_xg(matches_by_round):
    """
    Fetch xG for each finished match via /fixtures/statistics.
    Modifies matches_by_round in place.
    """
    total = sum(len(matches) for matches in matches_by_round.values())
    print(f"Obteniendo xG para {total} partidos...")

    count = 0
    for jornada_key in sorted(matches_by_round.keys(), key=lambda k: int(k.split('_')[1])):
        matches = matches_by_round[jornada_key]
        for match in matches:
            fixture_id = match.get("fixtureId")
            if fixture_id is None:
                continue

            home_xg, away_xg = fetch_fixture_xg(fixture_id)
            match["homeXg"] = home_xg
            match["awayXg"] = away_xg

            count += 1
            if count % 10 == 0:
                print(f"  Progreso xG: {count}/{total}")

            time.sleep(ApiPublic.RATE_LIMIT_DELAY)

    print(f"xG obtenido para {count} partidos.")


def build_upcoming_matches_json(fixtures):
    """
    Build upcoming matches JSON from API-Football fixtures.
    Filters for not-started matches (status.short == "NS").
    """
    upcoming_by_round = {}

    for fixture in fixtures:
        fixture_info = fixture.get("fixture", {})
        status = fixture_info.get("status", {})

        if status.get("short") != "NS":
            continue

        league = fixture.get("league", {})
        round_num = parse_round_number(league.get("round", ""))
        if round_num is None:
            continue

        fixture_id = fixture_info.get("id")
        teams = fixture.get("teams", {})

        home_team = teams.get("home", {})
        away_team = teams.get("away", {})

        match_date = fixture_info.get("date")

        jornada_key = f"jornada_{round_num}"
        if jornada_key not in upcoming_by_round:
            upcoming_by_round[jornada_key] = []

        match_data = {
            "fixtureId": fixture_id,
            "homeTeamId": home_team.get("id"),
            "awayTeamId": away_team.get("id"),
            "homeClub": home_team.get("name", ""),
            "awayClub": away_team.get("name", "")
        }
        if match_date:
            match_data["matchDate"] = match_date

        upcoming_by_round[jornada_key].append(match_data)

    return upcoming_by_round


def main():
    fixtures = fetch_laliga_fixtures()
    if fixtures is None:
        print("No se pudo obtener la lista de fixtures.")
        return 1

    matches_json = build_matches_json(fixtures)
    if not matches_json:
        print("No se generaron datos de partidos finalizados.")
    else:
        total_rounds = len(matches_json)
        total_matches = sum(len(v) for v in matches_json.values())
        print(f"Generados {total_matches} partidos finalizados en {total_rounds} jornadas.")

        enrich_matches_with_xg(matches_json)

        output_path = os.path.join(os.path.dirname(__file__), "matches.json")
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(matches_json, f, indent=2, ensure_ascii=False)
        print(f"Partidos finalizados guardados en: {output_path}")

    upcoming_json = build_upcoming_matches_json(fixtures)
    if not upcoming_json:
        print("No se generaron datos de partidos proximos.")
    else:
        total_rounds_upcoming = len(upcoming_json)
        total_upcoming = sum(len(v) for v in upcoming_json.values())
        print(f"Generados {total_upcoming} partidos proximos en {total_rounds_upcoming} jornadas.")

        upcoming_path = os.path.join(os.path.dirname(__file__), "upcoming_matches.json")
        with open(upcoming_path, "w", encoding="utf-8") as f:
            json.dump(upcoming_json, f, indent=2, ensure_ascii=False)
        print(f"Partidos proximos guardados en: {upcoming_path}")

    print("Completado con exito!")
    return 0


if __name__ == "__main__":
    sys.exit(main())
