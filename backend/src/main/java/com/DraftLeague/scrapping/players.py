import requests
import json
from teams import extract_players_teams
import os

def fetch_and_save_players(team_players_dict, output_path):
    """
    team_players_dict: dict con clave id equipo y valor lista de ids de jugadores
    output_path: ruta donde guardar el JSON resultante
    """
    all_players = []
    errores = []
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                      "AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/139.0.0.0 Safari/537.36",
        "X-Mas": ""
    }
    total = sum(len(ids) for ids in team_players_dict.values())
    count = 0
    for team_id, player_ids in team_players_dict.items():
        print(f"Procesando equipo {team_id} con {len(player_ids)} jugadores...")
        for player_id in player_ids:
            url = f"https://www.fotmob.com/api/data/playerData?id={player_id}"
            try:
                resp = requests.get(url, headers=headers, timeout=10)
                resp.raise_for_status()
                data = resp.json()
                pos_desc = data.get("positionDescription", {})
                position = None
                positions = pos_desc.get("positions", [])
                if positions and isinstance(positions, list):
                    pos_short = positions[0].get("strPosShort", {})
                    position = pos_short.get("label", None)
                avatar_url = "https://images.fotmob.com/image_resources/playerimages/"+str(player_id)+".png"
                player = {
                    "id": str(player_id),
                    "fullName": data.get("name", "Desconocido"),
                    "position": position,
                    "avatarUrl": avatar_url,
                    "teamId": team_id,
                }
                all_players.append(player)
                count += 1
                if count % 10 == 0 or count == total:
                    print(f"  Progreso: {count}/{total} jugadores procesados...")
            except Exception as e:
                print(f"Error con jugador {player_id}: {e}")
                errores.append((player_id, str(e)))
    if all_players:
        try:
            with open(output_path, "w", encoding="utf-8") as f:
                json.dump(all_players, f, ensure_ascii=False, indent=2)
            print(f"Se han guardado {len(all_players)} jugadores en {output_path}")
        except Exception as e:
            print(f"Error al guardar el archivo JSON: {e}")
    else:
        print("No se ha generado ningún JSON porque no se obtuvo ningún jugador.")
    if errores:
         print(f"Errores encontrados en {len(errores)} jugadores:")
         for pid, err in errores:
            print(f"  ID {pid}: {err}")

if __name__ == "__main__":
    team_players = json.loads(extract_players_teams())
    output_path = os.path.join(os.path.dirname(__file__), "players_data.json")
    fetch_and_save_players(team_players, output_path)
