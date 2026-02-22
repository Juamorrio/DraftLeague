import json
import os
import time
import requests
from public import ApiPublic


# API-Football position mapping to app positions
POSITION_MAP = {
    "Goalkeeper": "GK",
    "Defender": "CB",
    "Midfielder": "CM",
    "Attacker": "ST",
}


def fetch_and_save_players(output_path):
    """
    Fetch player data for all La Liga teams using API-Football squads endpoint.
    Uses /players/squads (1 request per team, 20 total).
    No per-player requests needed since squads returns name, photo, position.
    """
    all_players = []
    errores = []

    for team_id in ApiPublic.TEAM_IDS:
        try:
            url = f"{ApiPublic.BASE_URL}/players/squads"
            params = {"team": team_id}
            resp = requests.get(url, headers=ApiPublic.headers, params=params, timeout=15)
            resp.raise_for_status()
            data = resp.json()

            response = data.get("response", [])
            if not response:
                print(f"Sin datos para equipo {team_id}")
                continue

            squad = response[0].get("players", [])
            print(f"Procesando equipo {team_id} con {len(squad)} jugadores...")

            for player_data in squad:
                player_id = player_data.get("id")
                if not player_id:
                    continue

                api_position = player_data.get("position", "Midfielder")
                position = POSITION_MAP.get(api_position, "CM")

                avatar_url = player_data.get("photo", "")

                player = {
                    "id": str(player_id),
                    "fullName": player_data.get("name", "Desconocido"),
                    "position": position,
                    "marketValue": None,  # Not available in free tier
                    "avatarUrl": avatar_url,
                    "teamId": team_id,
                }
                all_players.append(player)

            time.sleep(ApiPublic.RATE_LIMIT_DELAY_SQUADS)  
        except Exception as e:
            print(f"Error con equipo {team_id}: {e}")
            errores.append((team_id, str(e)))

    if all_players:
        try:
            with open(output_path, "w", encoding="utf-8") as f:
                json.dump(all_players, f, ensure_ascii=False, indent=2)
            print(f"\nSe han guardado {len(all_players)} jugadores en {output_path}")
        except Exception as e:
            print(f"Error al guardar el archivo JSON: {e}")
    else:
        print("No se ha generado ningun JSON porque no se obtuvo ningun jugador.")

    if errores:
        print(f"\nErrores encontrados en {len(errores)} equipos:")
        for tid, err in errores:
            print(f"  Team {tid}: {err}")


if __name__ == "__main__":
    output_path = os.path.join(os.path.dirname(__file__), "players_data.json")
    fetch_and_save_players(output_path)
