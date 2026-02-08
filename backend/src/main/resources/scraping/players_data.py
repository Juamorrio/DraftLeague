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
        'was_fouled': 'wasFouled', 'fouls': 'foulsCommitted',
        # Goalkeeper-specific stats
        'saves': 'saves', 'goals_conceded': 'goalsConceded',
        'expected_goals_on_target_faced': 'xgotFaced', 'goals_prevented': 'goalsPrevented',
        'keeper_sweeper': 'sweeperActions', 'keeper_high_claim': 'highClaims',
        'keeper_diving_save': 'divingSaves', 'saves_inside_box': 'savesInsideBox',
        'punches': 'punches', 'player_throws': 'goalKeeperThrows',
        # Additional stats for all positions
        'headed_clearance': 'headedClearances', 'big_chance_missed_title': 'bigChancesMissed',
        'blocked_shots': 'blockedShots'
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
    
    def _map_fraction_stat(self, stats_dict: Dict, field_names, value):
        """Mapea estadísticas fraccionarias a campos individuales."""
        try:
            stats_dict[field_names[0]] = value['value']
            stats_dict[field_names[1]] = value['total']
        except (TypeError, KeyError, IndexError):
            try:
                stats_dict[field_names] = value['value'] if isinstance(value, dict) else value
            except TypeError:
                stats_dict[field_names[0]] = value
    
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
                    self._map_fraction_stat(stats_dict, self.FRACTION_MAPPING[stat_key], value)
        
        return stats_dict
    
    def _process_player_data(self, player_data: Dict, match_id: int) -> Optional[Dict]:
        """Procesa datos de un jugador y retorna sus estadísticas."""
        stats = self.parse_player_statistics(player_data, match_id)
        return stats if stats.get('playerId') else None
    
    def process_match(self, match_id: int) -> List[Dict[str, Any]]:
        match_data = self.get_match_details(match_id)
        if not match_data:
            return []
        
        player_stats = match_data.get('content', {}).get('playerStats')
        if not player_stats:
            print(f"   INFO: Sin campo playerStats")
            return []
        
        statistics_list = []
        
        if 'homeTeam' in player_stats:
            for team in ['homeTeam', 'awayTeam']:
                for player in player_stats.get(team, []):
                    if player.get('role'):
                        stats = self._process_player_data(player, match_id)
                        if stats:
                            statistics_list.append(stats)

        else:
            home_team_id = match_data.get('general', {}).get('homeTeam', {}).get('id')
            for player_id, player_data in player_stats.items():
                if player_data.get('stats'):
                    player_data.setdefault('id', int(player_id))
                    player_data['role'] = self._get_role_from_position(player_data.get('usualPosition'))
                    player_data['isHomeTeam'] = player_data.get('teamId') == home_team_id
                    stats = self._process_player_data(player_data, match_id)
                    if stats:
                        statistics_list.append(stats)
        
        return statistics_list
    
    def _get_role_from_position(self, position: int) -> str:
        return self.POSITION_MAP.get(position, "Midfielder")
    
    def _print_header(self, title: str):
        """Imprime un encabezado formateado."""
        print(f"\n{'='*70}\n{title}\n{'='*70}")
    
    def _save_and_report(self, statistics: List[Dict], jornada_key: str, output_file: str):
        """Guarda estadísticas en archivo y muestra resumen."""
        positions = {}
        for stat in statistics:
            pos = stat.get('playerType', 'UNKNOWN')
            positions[pos] = positions.get(pos, 0) + 1
        
        print(f"\nDistribución por posición:")
        for pos in sorted(positions.keys()):
            print(f"  {pos}: {positions[pos]} jugadores")
        
        output_path = output_file or os.path.join(os.path.dirname(__file__), f"{jornada_key}_stats.json")
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(statistics, f, indent=2, ensure_ascii=False)
        print(f"\nJSON guardado en: {output_path}")
        print(f"{len(statistics)} estadísticas listas para enviar al backend")
    
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
            match_id = match.get('matchId')
            home, away = match.get('homeTeamId'), match.get('awayTeamId')
            score = f"{match.get('homeScore')}-{match.get('awayScore')}"
            print(f"[{i}/{len(jornada_matches)}] Partido {match_id} ({home} vs {away}, {score})")
            
            stats = self.process_match(match_id)
            if stats:
                all_statistics.extend(stats)
                success_count += 1
                print(f"   OK: {len(stats)} jugadores procesados")
            else:
                print(f"   WARNING: Sin estadísticas disponibles")
        
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


def check_match(match_id: int):
    extractor = PlayerStatisticsExtractor()
    extractor._print_header(f"DIAGNÓSTICO DEL PARTIDO {match_id}")
    
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
    home = general.get('homeTeam', {}).get('name', 'N/A')
    away = general.get('awayTeam', {}).get('name', 'N/A')
    print(f"\nPartido: {home} vs {away}")
    print(f"Estado: {'Finalizado' if status.get('finished') else 'No finalizado'}")
    print(f"Resultado: {status.get('scoreStr', 'N/A')}")
    
    content = match_data.get('content', {})
    print(f"\nClaves en 'content': {list(content.keys())}")
    
    player_stats = content.get('playerStats')
    if not player_stats:
        print("\nEstadísticas de jugadores: No disponibles")
        possible = [k for k in content.keys() if any(x in k.lower() for x in ['player', 'lineup', 'stats'])]
        if possible:
            print(f"Claves alternativas: {possible}")
    else:
        home_players = player_stats.get('homeTeam', [])
        away_players = player_stats.get('awayTeam', [])
        total_with_role = sum(1 for p in home_players + away_players if p.get('role'))
        
        print(f"\nEstadísticas disponibles:")
        print(f"   - Equipo local: {len(home_players)} jugadores")
        print(f"   - Equipo visitante: {len(away_players)} jugadores")
        print(f"   - Total con rol: {total_with_role}")
        
        if home_players:
            example = home_players[0]
            name = example.get('name', {}).get('fullName', 'N/A')
            print(f"\nEjemplo (primer jugador local):")
            print(f"   - ID: {example.get('id', 'N/A')} | Nombre: {name}")
            print(f"   - Rol: {example.get('role', 'N/A')} | Stats: {'Si' if example.get('stats') else 'No'}")
    
    print("="*70 + "\n")


if __name__ == "__main__":
    import sys
    
    def print_usage():
        print("=" * 70)
        print("EXTRACTOR DE ESTADÍSTICAS POR JORNADA - FotMob API")
        print("=" * 70)
        print("\nUSO:")
        print("  python players_data.py <jornada> [output_file] [--send]")
        print("\nCOMANDOS ESPECIALES:")
        print("  --list                     Ver jornadas disponibles")
        print("  --check <match_id>         Verificar estado de un partido")
        print("\nOPCIONES:")
        print("  --send                     Enviar automaticamente al backend")
        print("\nEJEMPLOS:")
        print("  python players_data.py 1                    # Procesar jornada 1")
        print("  python players_data.py 1 custom.json        # Archivo custom")
        print("  python players_data.py 1 --send             # Procesar y enviar")
        print("  python players_data.py --list               # Ver jornadas")
        print("  python players_data.py --check 4837303      # Verificar partido\n")
    
    if len(sys.argv) < 2:
        print_usage()
        list_jornadas()
        sys.exit(0)
    
    command = sys.argv[1]
    
    if command == '--list':
        list_jornadas()
    elif command == '--check':
        if len(sys.argv) < 3:
            print("ERROR: Debes proporcionar un match ID\n   Uso: python players_data.py --check <match_id>")
            sys.exit(1)
        try:
            check_match(int(sys.argv[2]))
        except ValueError:
            print(f"ERROR: '{sys.argv[2]}' no es un número válido")
    else:
        output_file = next((arg for arg in sys.argv[2:] if not arg.startswith('--')), None)
        send_to_backend = '--send' in sys.argv
        
        stats = extract_jornada(command, output_file, send_to_backend)
        
        if stats:
            output_name = output_file or f"jornada_{command}_stats.json"
            print(f"\nEXITO: {len(stats)} estadísticas procesadas → {output_name}")
            if not send_to_backend:
                print(f"Envía con: python players_data.py {command} --send")
        else:
            print("\nERROR: No se encontraron estadísticas para procesar")


