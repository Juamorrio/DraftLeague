import json
import time
import requests
from public import ApiPublic


def extract_players_teams():
    """
    Fetch squad player IDs for each La Liga team using API-Football.
    Endpoint: GET /players/squads?team={id}
    Uses 1 request per team (20 teams = 20 requests).
    """
    team_players = {}

    for team_id in ApiPublic.TEAM_IDS:
        try:
            url = f"{ApiPublic.BASE_URL}/players/squads"
            params = {"team": team_id}
            resp = requests.get(url, headers=ApiPublic.headers, params=params, timeout=15)
            resp.raise_for_status()
            data = resp.json()

            response = data.get("response", [])
            if not response:
                print(f"Sin datos de plantilla para equipo {team_id}")
                team_players[team_id] = []
                continue

            squad = response[0].get("players", [])
            player_ids = [p["id"] for p in squad if p.get("id")]

            seen = set()
            unique_ids = []
            for pid in player_ids:
                if pid not in seen:
                    seen.add(pid)
                    unique_ids.append(pid)

            team_players[team_id] = unique_ids
            print(f"Equipo {team_id}: {len(unique_ids)} jugadores")

            time.sleep(ApiPublic.RATE_LIMIT_DELAY_SQUADS) 
        except requests.RequestException as e:
            print(f"Error de red para equipo {team_id}: {e}")
            team_players[team_id] = []
        except Exception as e:
            print(f"Error al procesar equipo {team_id}: {e}")
            team_players[team_id] = []

    return json.dumps(team_players)


if __name__ == "__main__":
    result = extract_players_teams()
    print(f"\nResultado: {len(json.loads(result))} equipos procesados")
