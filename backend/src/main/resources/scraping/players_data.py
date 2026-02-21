import requests
import json
import os
import time
from typing import Dict, List, Any, Optional
from public import ApiPublic


class PlayerStatisticsExtractor:
    """
    Extracts per-match player statistics from API-Football.
    Endpoint: GET /fixtures/players?fixture={id}
    Uses 1 request per fixture (10 per jornada).
    """

    POSITION_MAP = {
        "G": "GOALKEEPER",
        "D": "DEFENDER",
        "M": "MIDFIELDER",
        "F": "FORWARD",
    }

    def __init__(self, backend_url: str = "http://localhost:8080"):
        self.backend_url = backend_url
        self.matches_file = os.path.join(os.path.dirname(__file__), "matches.json")

    def load_matches(self) -> Dict[str, List[Dict]]:
        try:
            with open(self.matches_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except (FileNotFoundError, json.JSONDecodeError) as e:
            print(f"ERROR: {e}. Ejecuta primero: python home.py")
            return {}

    def get_available_jornadas(self) -> List[str]:
        return sorted(self.load_matches().keys())

    def determine_player_type(self, position_code: str) -> str:
        return self.POSITION_MAP.get(position_code, "MIDFIELDER")

    def get_fixture_players(self, fixture_id: int) -> Optional[List]:
        """
        Fetch player statistics for a fixture from API-Football.
        Returns the response array: [home_team_data, away_team_data]
        """
        try:
            url = f"{ApiPublic.BASE_URL}/fixtures/players"
            params = {"fixture": fixture_id}
            response = requests.get(url, headers=ApiPublic.headers, params=params, timeout=30)
            response.raise_for_status()
            data = response.json()
            return data.get("response", [])
        except requests.RequestException as e:
            print(f"   WARNING: Error en fixture {fixture_id}: {e}")
            return None

    def safe_int(self, value) -> Optional[int]:
        if value is None:
            return None
        try:
            return int(value)
        except (ValueError, TypeError):
            return None

    def safe_float(self, value) -> Optional[float]:
        if value is None:
            return None
        try:
            return float(value)
        except (ValueError, TypeError):
            return None

    def parse_player_stats(self, player_data: Dict, fixture_id: int, is_home: bool) -> Optional[Dict[str, Any]]:
        """
        Parse a single player's statistics from the API-Football response.
        The player_data has structure: {player: {id, name, photo}, statistics: [{...}]}
        """
        player_info = player_data.get("player", {})
        player_id = player_info.get("id")
        if not player_id:
            return None

        stats_list = player_data.get("statistics", [])
        if not stats_list:
            return None

        s = stats_list[0]

        games = s.get("games", {})
        shots = s.get("shots", {})
        goals_data = s.get("goals", {})
        passes = s.get("passes", {})
        tackles = s.get("tackles", {})
        duels = s.get("duels", {})
        dribbles = s.get("dribbles", {})
        fouls = s.get("fouls", {})
        cards = s.get("cards", {})
        penalty = s.get("penalty", {})

        position_code = games.get("position", "M")
        if position_code is None:
            position_code = "M"
        pos_map_full = {"Goalkeeper": "G", "Defender": "D", "Midfielder": "M", "Forward": "F"}
        position_code = pos_map_full.get(position_code, position_code[0] if position_code else "M")

        player_type = self.determine_player_type(position_code)

        total_passes = self.safe_int(passes.get("total"))
        pass_accuracy = self.safe_int(passes.get("accuracy"))
        accurate_passes = None
        if total_passes is not None and pass_accuracy is not None and total_passes > 0:
            accurate_passes = round(total_passes * pass_accuracy / 100)

        duels_total = self.safe_int(duels.get("total"))
        duels_won = self.safe_int(duels.get("won"))
        duels_lost = None
        if duels_total is not None and duels_won is not None:
            duels_lost = duels_total - duels_won

        stats_dict = {
            "playerType": player_type,
            "playerId": str(player_id),
            "fixtureId": fixture_id,
            "isHomeTeam": is_home,

            "rating": self.safe_float(games.get("rating")),
            "minutesPlayed": self.safe_int(games.get("minutes")) or 0,
            "goals": self.safe_int(goals_data.get("total")),
            "assists": self.safe_int(goals_data.get("assists")),
            "totalShots": self.safe_int(shots.get("total")),
            "shotsOnTarget": self.safe_int(shots.get("on")),
            "chancesCreated": self.safe_int(passes.get("key")),
            "totalPasses": total_passes,
            "accuratePasses": accurate_passes,
            "tackles": self.safe_int(tackles.get("total")),
            "blocks": self.safe_int(tackles.get("blocks")),
            "interceptions": self.safe_int(tackles.get("interceptions")),
            "duelsWon": duels_won,
            "duelsLost": duels_lost,
            "successfulDribbles": self.safe_int(dribbles.get("success")),
            "totalDribbles": self.safe_int(dribbles.get("attempts")),
            "dribbledPast": self.safe_int(dribbles.get("past")),
            "wasFouled": self.safe_int(fouls.get("drawn")),
            "foulsCommitted": self.safe_int(fouls.get("committed")),
            "yellowCards": self.safe_int(cards.get("yellow")),
            "redCards": self.safe_int(cards.get("red")),
            "offsides": self.safe_int(s.get("offsides")),
            "penaltyScored": self.safe_int(penalty.get("scored")),
            "penaltyMissed": self.safe_int(penalty.get("missed")),
            "penaltiesWon": self.safe_int(penalty.get("won")),
            "isSubstitute": games.get("substitute", False),
            "isCaptain": games.get("captain", False),
            "shirtNumber": self.safe_int(games.get("number")),
            "penaltyCommitted": self.safe_int(penalty.get("commited")),  # API typo: "commited"
        }

        if player_type == "GOALKEEPER":
            stats_dict["saves"] = self.safe_int(goals_data.get("saves"))
            stats_dict["goalsConceded"] = self.safe_int(goals_data.get("conceded"))
            stats_dict["penaltiesSaved"] = self.safe_int(penalty.get("saved"))

        return stats_dict

    def process_fixture(self, fixture_id: int) -> List[Dict[str, Any]]:
        """Process a single fixture and return all player statistics."""
        response = self.get_fixture_players(fixture_id)
        if not response:
            return []

        statistics_list = []

        for team_idx, team_data in enumerate(response):
            is_home = (team_idx == 0)
            players = team_data.get("players", [])

            for player_data in players:
                stats = self.parse_player_stats(player_data, fixture_id, is_home)
                if stats and stats.get("minutesPlayed", 0) > 0:
                    statistics_list.append(stats)

        return statistics_list

    def _print_header(self, title: str):
        print(f"\n{'='*70}\n{title}\n{'='*70}")

    def _save_and_report(self, statistics: List[Dict], jornada_key: str, output_file: str):
        positions = {}
        for stat in statistics:
            pos = stat.get('playerType', 'UNKNOWN')
            positions[pos] = positions.get(pos, 0) + 1

        print(f"\nDistribucion por posicion:")
        for pos in sorted(positions.keys()):
            print(f"  {pos}: {positions[pos]} jugadores")

        output_path = output_file or os.path.join(os.path.dirname(__file__), f"{jornada_key}_stats.json")
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(statistics, f, indent=2, ensure_ascii=False)
        print(f"\nJSON guardado en: {output_path}")
        print(f"{len(statistics)} estadisticas listas para enviar al backend")

    def process_jornada(self, jornada: str, output_file: str = None) -> List[Dict[str, Any]]:
        matches = self.load_matches()
        jornada_key = jornada if jornada.startswith('jornada_') else f'jornada_{jornada}'

        if jornada_key not in matches:
            print(f"ERROR: Jornada '{jornada_key}' no encontrada")
            print(f"   Jornadas disponibles: {', '.join(self.get_available_jornadas())}")
            return []

        jornada_matches = matches[jornada_key]
        self._print_header(f"PROCESANDO {jornada_key.upper()}")
        print(f"{len(jornada_matches)} partidos a procesar\n")

        all_statistics = []
        success_count = 0

        for i, match in enumerate(jornada_matches, 1):
            fixture_id = match.get('fixtureId')
            home = match.get('homeClub', match.get('homeTeamId'))
            away = match.get('awayClub', match.get('awayTeamId'))
            score = f"{match.get('homeScore')}-{match.get('awayScore')}"
            print(f"[{i}/{len(jornada_matches)}] Fixture {fixture_id} ({home} vs {away}, {score})")

            stats = self.process_fixture(fixture_id)
            if stats:
                all_statistics.extend(stats)
                success_count += 1
                print(f"   OK: {len(stats)} jugadores procesados")
            else:
                print(f"   WARNING: Sin estadisticas disponibles")

            if i < len(jornada_matches):
                time.sleep(ApiPublic.RATE_LIMIT_DELAY)

        self._print_header(f"RESUMEN DE {jornada_key.upper()}")
        print(f"Partidos procesados: {success_count}/{len(jornada_matches)}")
        print(f"Total jugadores: {len(all_statistics)}")

        if all_statistics:
            self._save_and_report(all_statistics, jornada_key, output_file)

        print(f"{'='*70}\n")
        return all_statistics

    def send_bulk_statistics(self, statistics_list: List[Dict[str, Any]]) -> bool:
        try:
            response = requests.post(f"{self.backend_url}/api/statistics/bulk", json=statistics_list, timeout=60)
            response.raise_for_status()
            return True
        except requests.RequestException as e:
            print(f"   ERROR: {e}")
            return False


def extract_jornada(jornada: str, output_file: str = None, send_to_backend: bool = False):
    extractor = PlayerStatisticsExtractor()
    statistics = extractor.process_jornada(jornada, output_file)

    if statistics and send_to_backend:
        print(f"\nEnviando estadisticas al backend...")
        success = extractor.send_bulk_statistics(statistics)
        print("OK: Estadisticas enviadas correctamente" if success else "ERROR: Fallo al enviar estadisticas")

    return statistics


def list_jornadas():
    jornadas = PlayerStatisticsExtractor().get_available_jornadas()
    if not jornadas:
        print("ERROR: No hay jornadas disponibles. Ejecuta primero: python home.py")
        return
    print(f"\nJORNADAS DISPONIBLES EN MATCHES.JSON\n{'='*50}")
    print('\n'.join(f"  - {j}" for j in jornadas))
    print(f"{'='*50}\nTotal: {len(jornadas)} jornadas\n")


if __name__ == "__main__":
    import sys

    def print_usage():
        print("=" * 70)
        print("EXTRACTOR DE ESTADISTICAS POR JORNADA - API-Football")
        print("=" * 70)
        print("\nUSO:")
        print("  python players_data.py <jornada> [output_file] [--send]")
        print("\nCOMANDOS ESPECIALES:")
        print("  --list                     Ver jornadas disponibles")
        print("\nOPCIONES:")
        print("  --send                     Enviar automaticamente al backend")
        print("\nEJEMPLOS:")
        print("  python players_data.py 1                    # Procesar jornada 1")
        print("  python players_data.py 1 custom.json        # Archivo custom")
        print("  python players_data.py 1 --send             # Procesar y enviar")
        print("  python players_data.py --list               # Ver jornadas\n")

    if len(sys.argv) < 2:
        print_usage()
        list_jornadas()
        sys.exit(0)

    command = sys.argv[1]

    if command == '--list':
        list_jornadas()
    else:
        output_file = next((arg for arg in sys.argv[2:] if not arg.startswith('--')), None)
        send_to_backend = '--send' in sys.argv

        stats = extract_jornada(command, output_file, send_to_backend)

        if stats:
            output_name = output_file or f"jornada_{command}_stats.json"
            print(f"\nEXITO: {len(stats)} estadisticas procesadas -> {output_name}")
            if not send_to_backend:
                print(f"Envia con: python players_data.py {command} --send")
        else:
            print("\nERROR: No se encontraron estadisticas para procesar")
