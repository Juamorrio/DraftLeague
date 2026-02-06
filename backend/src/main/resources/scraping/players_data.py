import requests
import json
import os
from typing import Dict, List, Any, Optional

class PlayerStatisticsExtractor:
    STAT_MAPPING = {
        'rating_title': 'fotmobRating', 'minutes_played': 'minutesPlayed', 'goals': 'goals',
        'assists': 'assists', 'total_shots': 'totalShots', 'chances_created': 'chancesCreated',
        'penalties_won': 'penaltiesWon', 'expected_goals': 'expectedGoals',
        'expected_goals_on_target_variant': 'expectedGoalsOnTarget', 'expected_assists': 'expectedAssists',
        'xg_and_xa': 'xgAndXa', 'defensive_actions': 'defensiveActions', 'touches': 'touches',
        'touches_opp_box': 'touchesOppBox', 'passes_into_final_third': 'passesIntoFinalThird',
        'corners': 'corners', 'dispossessed': 'dispossessed', 'expected_goals_non_penalty': 'xgNonPenalty',
        'matchstats.headers.tackles': 'tackles', 'shot_blocks': 'blocks', 'clearances': 'clearances',
        'interceptions': 'interceptions', 'recoveries': 'recoveries', 'dribbled_past': 'dribbledPast',
        'was_fouled': 'wasFouled', 'fouls': 'foulsCommitted'
    }
    
    FRACTION_MAPPING = {
        'accurate_passes': ('accuratePasses', 'totalPasses'),
        'ShotsOnTarget': ('shotsOnTarget', 'totalShotsOnTarget'),
        'dribbles_succeeded': ('successfulDribbles', 'totalDribbles'),
        'accurate_crosses': ('accurateCrosses', 'totalCrosses'),
        'long_balls_accurate': ('accurateLongBalls', 'totalLongBalls'),
        'ground_duels_won': ('groundDuelsWon', 'totalGroundDuels'),
        'aerials_won': ('aerialDuelsWon', 'totalAerialDuels'),
        'duel_won': 'duelsWon', 'duel_lost': 'duelsLost'
    }
    
    POSITION_MAP = {0: "Goalkeeper", 1: "Defender", 2: "Midfielder", 3: "Attacker"}
    
    def __init__(self, backend_url: str = "http://localhost:8080"):
        self.backend_url = backend_url
        self.fotmob_api_url = "https://www.fotmob.com/api"
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
    
    def determine_player_type(self, role: str) -> str:
        role_lower = (role or "").lower()
        if "goalkeeper" in role_lower or "keeper" in role_lower:
            return "GOALKEEPER"
        if "defender" in role_lower or "defence" in role_lower:
            return "DEFENDER"
        if "midfielder" in role_lower or "midfield" in role_lower:
            return "MIDFIELDER"
        if "attacker" in role_lower or "forward" in role_lower or "striker" in role_lower:
            return "FORWARD"
        return "MIDFIELDER"
    
    def get_match_details(self, match_id: int) -> Optional[Dict[str, Any]]:
        try:
            response = requests.get(f"{self.fotmob_api_url}/matchDetails?matchId={match_id}", timeout=30)
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"   WARNING: Error en partido {match_id}: {e}")
            return None
    
    def extract_stat_value(self, stat: Dict[str, Any]) -> Any:
        if not stat or 'stat' not in stat:
            return None
        stat_data = stat['stat']
        if stat_data.get('type') == 'fractionWithPercentage':
            return {'value': stat_data.get('value'), 'total': stat_data.get('total')}
        if stat_data.get('type') in ['integer', 'double', 'boolean']:
            return stat_data.get('value')
        return None
    
    def parse_player_statistics(self, player_data: Dict[str, Any], match_id: int) -> Dict[str, Any]:
        role = player_data.get('role', '')
        stats_dict = {
            'playerType': self.determine_player_type(role),
            'playerId': player_data.get('id'),
            'matchId': match_id,
            'isHomeTeam': player_data.get('isHomeTeam', False),
            'role': role
        }
        
        for stat_category in player_data.get('stats', []):
            for stat_data in stat_category.get('stats', {}).values():
                value = self.extract_stat_value(stat_data)
                if value is None:
                    continue
                
                stat_key = stat_data.get('key')
                if stat_key in self.STAT_MAPPING:
                    stats_dict[self.STAT_MAPPING[stat_key]] = value
                elif stat_key in self.FRACTION_MAPPING:
                    field_names = self.FRACTION_MAPPING[stat_key]
                    if isinstance(value, dict) and isinstance(field_names, tuple):
                        stats_dict[field_names[0]] = value['value']
                        stats_dict[field_names[1]] = value['total']
                    elif isinstance(value, dict):
                        stats_dict[field_names] = value['value']
                    elif isinstance(field_names, tuple):
                        stats_dict[field_names[0]] = value
                    else:
                        stats_dict[field_names] = value
        
        return stats_dict
    
    def process_match(self, match_id: int) -> List[Dict[str, Any]]:
        match_data = self.get_match_details(match_id)
        if not match_data:
            return []
        
        player_stats = match_data.get('content', {}).get('playerStats')
        if not player_stats:
            print(f"   INFO: Sin campo playerStats")
            return []
        
        statistics_list = []
        
        # Formato diccionario con IDs como claves
        if isinstance(player_stats, dict) and 'homeTeam' not in player_stats:
            home_team_id = match_data.get('general', {}).get('homeTeam', {}).get('id')
            for player_id, player_data in player_stats.items():
                if not player_data.get('stats'):
                    continue
                player_data['id'] = player_data.get('id', int(player_id))
                player_data['role'] = self._get_role_from_position(player_data.get('usualPosition'))
                player_data['isHomeTeam'] = player_data.get('teamId') == home_team_id
                stats = self.parse_player_statistics(player_data, match_id)
                if stats.get('playerId'):
                    statistics_list.append(stats)
        # Formato con homeTeam/awayTeam
        else:
            for team in ['homeTeam', 'awayTeam']:
                for player in player_stats.get(team, []):
                    if not player.get('role'):
                        continue
                    stats = self.parse_player_statistics(player, match_id)
                    if stats.get('playerId'):
                        statistics_list.append(stats)
        
        return statistics_list
    
    def _get_role_from_position(self, position: int) -> str:
        return self.POSITION_MAP.get(position, "Midfielder")
    
    def process_jornada(self, jornada: str, output_file: str = None) -> List[Dict[str, Any]]:
        matches = self.load_matches()
        jornada_key = jornada if jornada.startswith('jornada_') else f'jornada_{jornada}'
        
        if jornada_key not in matches:
            print(f"ERROR: Jornada '{jornada_key}' no encontrada")
            print(f"   Jornadas disponibles: {', '.join(self.get_available_jornadas())}")
            return []
        
        jornada_matches = matches[jornada_key]
        print(f"\n{'='*70}\nPROCESANDO {jornada_key.upper()}\n{'='*70}")
        print(f"{len(jornada_matches)} partidos a procesar\n")
        
        all_statistics = []
        success_count = 0
        
        for i, match in enumerate(jornada_matches, 1):
            match_id = match.get('matchId')
            print(f"[{i}/{len(jornada_matches)}] Partido {match_id} ({match.get('homeTeamId')} vs {match.get('awayTeamId')}, {match.get('homeScore')}-{match.get('awayScore')})")
            
            stats = self.process_match(match_id)
            if stats:
                all_statistics.extend(stats)
                success_count += 1
                print(f"   OK: {len(stats)} jugadores procesados")
            else:
                print(f"   WARNING: Sin estadísticas disponibles")
        
        print(f"\n{'='*70}\nRESUMEN DE {jornada_key.upper()}\n{'='*70}")
        print(f"Partidos procesados: {success_count}/{len(jornada_matches)}")
        print(f"Total jugadores: {len(all_statistics)}")
        
        if all_statistics:
            positions = {}
            for stat in all_statistics:
                pos = stat.get('playerType', 'UNKNOWN')
                positions[pos] = positions.get(pos, 0) + 1
            
            print(f"\nDistribución por posición:")
            for pos in sorted(positions.keys()):
                print(f"  {pos}: {positions[pos]} jugadores")
            
            output_file = output_file or os.path.join(os.path.dirname(__file__), f"{jornada_key}_stats.json")
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(all_statistics, f, indent=2, ensure_ascii=False)
            print(f"\nJSON guardado en: {output_file}")
            print(f"{len(all_statistics)} estadísticas listas para enviar al backend")
        
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


def extract_jornada(jornada: str, output_file: str = None):
    return PlayerStatisticsExtractor().process_jornada(jornada, output_file)


def list_jornadas():
    jornadas = PlayerStatisticsExtractor().get_available_jornadas()
    if not jornadas:
        print("ERROR: No hay jornadas disponibles. Ejecuta primero: python home.py")
        return
    print(f"\nJORNADAS DISPONIBLES EN MATCHES.JSON\n{'='*50}")
    for jornada in jornadas:
        print(f"  - {jornada}")
    print(f"{'='*50}\nTotal: {len(jornadas)} jornadas\n")


def check_match(match_id: int):
    print(f"\nDIAGNÓSTICO DEL PARTIDO {match_id}\n{'='*70}")
    
    extractor = PlayerStatisticsExtractor()
    match_data = extractor.get_match_details(match_id)
    
    if not match_data:
        print("ERROR: No se pudo obtener información del partido")
        return
    
    debug_file = os.path.join(os.path.dirname(__file__), f"match_{match_id}_debug.json")
    with open(debug_file, 'w', encoding='utf-8') as f:
        json.dump(match_data, f, indent=2, ensure_ascii=False)
    print(f"Respuesta completa guardada en: {debug_file}")
    
    general = match_data.get('general', {})
    status = general.get('status', {})
    print(f"\nPartido: {general.get('homeTeam', {}).get('name', 'N/A')} vs {general.get('awayTeam', {}).get('name', 'N/A')}")
    print(f"Estado: {'Finalizado' if status.get('finished') else 'No finalizado'}")
    print(f"Resultado: {status.get('scoreStr', 'N/A')}")
    
    content = match_data.get('content', {})
    print(f"\nClaves en 'content': {list(content.keys())}")
    
    player_stats = content.get('playerStats')
    print(f"\nEstadísticas de jugadores:")
    if not player_stats:
        print("No disponibles (campo 'playerStats' no existe)")
        possible_keys = [k for k in content.keys() if 'player' in k.lower() or 'lineup' in k.lower() or 'stats' in k.lower()]
        if possible_keys:
            print(f"\nClaves alternativas encontradas: {possible_keys}")
    else:
        home_players = player_stats.get('homeTeam', [])
        away_players = player_stats.get('awayTeam', [])
        print(f"Disponibles")
        print(f"   - Equipo local: {len(home_players)} jugadores")
        print(f"   - Equipo visitante: {len(away_players)} jugadores")
        print(f"   - Total con rol: {sum(1 for p in home_players if p.get('role')) + sum(1 for p in away_players if p.get('role'))}")
        
        if home_players:
            example = home_players[0]
            print(f"\nEjemplo de jugador (primero del equipo local):")
            print(f"   - ID: {example.get('id', 'N/A')}")
            print(f"   - Nombre: {example.get('name', {}).get('fullName', 'N/A')}")
            print(f"   - Rol: {example.get('role', 'N/A')}")
            print(f"   - Tiene stats: {'Si' if example.get('stats') else 'No'}")
    
    print("="*70 + "\n")


if __name__ == "__main__":
    import sys
    
    print("=" * 70)
    print("EXTRACTOR DE ESTADÍSTICAS POR JORNADA - FotMob API")
    print("=" * 70)
    
    if len(sys.argv) < 2:
        print("\nUSO:")
        print("  python players_data.py <jornada> [output_file]")
        print("\nCOMANDOS ESPECIALES:")
        print("  --list                     Ver jornadas disponibles")
        print("  --check <match_id>         Verificar estado de un partido")
        print("\nEJEMPLOS:")
        print("  python players_data.py 1              # Procesar jornada 1 -> jornada_1_stats.json")
        print("  python players_data.py 1 custom.json  # Especificar nombre del archivo")
        print("  python players_data.py --list         # Ver jornadas disponibles")
        print("  python players_data.py --check 4837303 # Verificar estado de un partido\n")
        list_jornadas()
        sys.exit(0)
    
    if sys.argv[1] == '--list':
        list_jornadas()
    elif sys.argv[1] == '--check':
        if len(sys.argv) < 3:
            print("ERROR: Debes proporcionar un match ID\n   Uso: python players_data.py --check <match_id>")
            sys.exit(1)
        try:
            check_match(int(sys.argv[2]))
        except ValueError:
            print(f"ERROR: '{sys.argv[2]}' no es un número válido")
    else:
        stats = extract_jornada(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
        if stats:
            output_name = sys.argv[2] if len(sys.argv) > 2 else f"jornada_{sys.argv[1]}_stats.json"
            print(f"\nEXITO: {len(stats)} estadísticas procesadas")
            print(f"Archivo: {output_name}")
            print(f"Envía al backend con: POST {output_name} -> /api/statistics/bulk")
        else:
            print("\nERROR: No se encontraron estadísticas para procesar")

