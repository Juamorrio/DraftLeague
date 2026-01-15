from bs4 import BeautifulSoup
import os
import requests


import json
import os, ssl
import urllib

if (not os.environ.get('PYTHONHTTPSVERIFY', '') and
getattr(ssl, '_create_unverified_context', None)):
    ssl._create_default_https_context = ssl._create_unverified_context




def extract_players_teams():
    fotmobteams: str = "https://www.fotmob.com/api/data/teams?id="
    headers: dict = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        "AppleWebKit/537.36 (KHTML, like Gecko) "
                        "Chrome/139.0.0.0 Safari/537.36",
        "X-Mas": ""
    }

    def collect_player_ids(data):
        ids = []
        if not isinstance(data, dict):
            return ids
        squad = data.get('squad')
        if isinstance(squad, dict):
            sections = squad.get('squad')
            if isinstance(sections, list):
                for section in sections:
                    members = section.get('members') if isinstance(section, dict) else None
                    if isinstance(members, list):
                        for m in members:
                            pid = m.get('id') if isinstance(m, dict) else None
                            if isinstance(pid, int):
                                ids.append(pid)
        seen = set()
        out = []
        for pid in ids:
            if pid not in seen:
                seen.add(pid)
                out.append(pid)
        return out


    squads_ids = [8634, 8633, 9906, 10205, 8558, 8603, 9910, 8315, 10268, 10268, 8370, 8560, 8305, 7732, 8302, 8371, 9866, 8661, 10267, 8581, 8670]
    team_players = {}
    for team_id in squads_ids:
        try:
            resp = requests.get(fotmobteams + str(team_id), headers=headers, timeout=15)
            resp.raise_for_status()
            data = resp.json()
            player_ids = collect_player_ids(data)
            coach_id = None
            staff = data.get('staff')
            if isinstance(staff, list):
                for member in staff:
                    if isinstance(member, dict) and member.get('role') == 'Coach':
                        cid = member.get('id')
                        if isinstance(cid, int):
                            coach_id = cid
                            break
            ids = list(player_ids)
            if coach_id is not None:
                ids.append(coach_id)
            team_players[team_id] = ids
        except requests.RequestException as e:
            print(f"Network/HTTP error for team {team_id}:", e)
            team_players[team_id] = []
        except Exception as e:
            print(f"Error al procesar el equipo {team_id}:", e)
            team_players[team_id] = []
    return json.dumps(team_players)


    
    