import requests
import json
import os
from typing import Dict, List, Any, Optional

class PlayerStatisticsExtractor:
    """
    Extrae estadísticas de jugadores de FotMob API.
    Trabaja directamente con matches.json procesando jornadas completas.
    """
    
    def __init__(self, backend_url: str = "http://localhost:8080"):
        self.backend_url = backend_url
        self.fotmob_api_url = "https://www.fotmob.com/api"
        self.matches_file = os.path.join(os.path.dirname(__file__), "matches.json")
    
    def load_matches(self) -> Dict[str, List[Dict]]:
        """Carga el archivo matches.json con los partidos por jornada."""
        try:
            with open(self.matches_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except FileNotFoundError:
            print(f"❌ Error: No se encontró {self.matches_file}")
            print("   Ejecuta primero: python home.py")
            return {}
        except json.JSONDecodeError:
            print(f"❌ Error: {self.matches_file} no es un JSON válido")
            return {}
    
    def get_available_jornadas(self) -> List[str]:
        """Obtiene la lista de jornadas disponibles en matches.json."""
        matches = self.load_matches()
        return sorted(matches.keys())
    
    def determine_player_type(self, role: str) -> str:
        """Determina el tipo de jugador basándose en su rol."""
        role_lower = role.lower() if role else ""
        
        if "goalkeeper" in role_lower or "keeper" in role_lower:
            return "GOALKEEPER"
        elif "defender" in role_lower or "defence" in role_lower:
            return "DEFENDER"
        elif "midfielder" in role_lower or "midfield" in role_lower:
            return "MIDFIELDER"
        elif "attacker" in role_lower or "forward" in role_lower or "striker" in role_lower:
            return "FORWARD"
        else:
            return "MIDFIELDER"
    
    def get_match_details(self, match_id: int) -> Optional[Dict[str, Any]]:
        """Obtiene los detalles de un partido desde FotMob API."""
        try:
            url = f"{self.fotmob_api_url}/matchDetails?matchId={match_id}"
            response = requests.get(url, timeout=30)
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"   ⚠️ Error en partido {match_id}: {e}")
            return None
    
    def extract_stat_value(self, stat: Dict[str, Any]) -> Any:
        """Extrae el valor de una estadística según su tipo."""
        if not stat or 'stat' not in stat:
            return None
            
        stat_data = stat['stat']
        stat_type = stat_data.get('type')
        value = stat_data.get('value')
        
        if stat_type == 'fractionWithPercentage':
            return {'value': value, 'total': stat_data.get('total')}
        elif stat_type in ['integer', 'double', 'boolean']:
            return value
        
        return None
    
    def parse_player_statistics(self, player_data: Dict[str, Any], match_id: int) -> Dict[str, Any]:
        """Convierte datos de un jugador al formato del backend."""
        role = player_data.get('role', '')
        player_type = self.determine_player_type(role)
        
        stats_dict = {
            'playerType': player_type,
            'playerId': player_data.get('id'),
            'matchId': match_id,
            'isHomeTeam': player_data.get('isHomeTeam', False),
            'role': role
        }
        
        # Mapeo de claves de la API a campos del backend
        stat_mapping = {
            'rating_title': 'fotmobRating',
            'minutes_played': 'minutesPlayed',
            'goals': 'goals',
            'assists': 'assists',
            'total_shots': 'totalShots',
            'chances_created': 'chancesCreated',
            'penalties_won': 'penaltiesWon',
            'expected_goals': 'expectedGoals',
            'expected_goals_on_target_variant': 'expectedGoalsOnTarget',
            'expected_assists': 'expectedAssists',
            'xg_and_xa': 'xgAndXa',
            'defensive_actions': 'defensiveActions',
            'touches': 'touches',
            'touches_opp_box': 'touchesOppBox',
            'passes_into_final_third': 'passesIntoFinalThird',
            'corners': 'corners',
            'dispossessed': 'dispossessed',
            'expected_goals_non_penalty': 'xgNonPenalty',
            'matchstats.headers.tackles': 'tackles',
            'shot_blocks': 'blocks',
            'clearances': 'clearances',
            'interceptions': 'interceptions',
            'recoveries': 'recoveries',
            'dribbled_past': 'dribbledPast',
            'was_fouled': 'wasFouled',
            'fouls': 'foulsCommitted'
        }
        
        # Mapeo para estadísticas con fracciones
        fraction_mapping = {
            'accurate_passes': ('accuratePasses', 'totalPasses'),
            'ShotsOnTarget': ('shotsOnTarget', 'totalShotsOnTarget'),
            'dribbles_succeeded': ('successfulDribbles', 'totalDribbles'),
            'accurate_crosses': ('accurateCrosses', 'totalCrosses'),
            'long_balls_accurate': ('accurateLongBalls', 'totalLongBalls'),
            'ground_duels_won': ('groundDuelsWon', 'totalGroundDuels'),
            'aerials_won': ('aerialDuelsWon', 'totalAerialDuels'),
            'duel_won': 'duelsWon',
            'duel_lost': 'duelsLost'
        }
        
        # Procesar estadísticas
        for stat_category in player_data.get('stats', []):
            for stat_name, stat_data in stat_category.get('stats', {}).items():
                value = self.extract_stat_value(stat_data)
                if value is None:
                    continue
                
                stat_key = stat_data.get('key')
                
                # Mapeo simple
                if stat_key in stat_mapping:
                    stats_dict[stat_mapping[stat_key]] = value
                # Mapeo con fracciones
                elif stat_key in fraction_mapping:
                    if isinstance(value, dict):
                        field_names = fraction_mapping[stat_key]
                        if isinstance(field_names, tuple):
                            stats_dict[field_names[0]] = value['value']
                            stats_dict[field_names[1]] = value['total']
                        else:
                            stats_dict[field_names] = value['value']
                    else:
                        if isinstance(fraction_mapping[stat_key], tuple):
                            stats_dict[fraction_mapping[stat_key][0]] = value
                        else:
                            stats_dict[fraction_mapping[stat_key]] = value
        
        return stats_dict
    
    def process_match(self, match_id: int) -> List[Dict[str, Any]]:
        """Procesa un partido y extrae estadísticas de todos los jugadores."""
        match_data = self.get_match_details(match_id)
        if not match_data:
            return []
        
        # Verificar si el partido tiene estadísticas disponibles
        content = match_data.get('content', {})
        player_stats = content.get('playerStats')
        
        if not player_stats:
            print(f"   ℹ️ Sin campo playerStats")
            return []
        
        statistics_list = []
        
        # CASO 1: Formato diccionario con IDs como claves (formato actual de FotMob)
        if isinstance(player_stats, dict) and not ('homeTeam' in player_stats or 'awayTeam' in player_stats):
            for player_id, player_data in player_stats.items():
                # Verificar que tenga estadísticas
                if not player_data.get('stats'):
                    continue
                
                # Adaptar formato al esperado
                player_data['id'] = player_data.get('id', int(player_id))
                player_data['role'] = self._get_role_from_position(player_data.get('usualPosition'))
                
                # Determinar si es equipo local basado en el teamId
                home_team_id = match_data.get('general', {}).get('homeTeam', {}).get('id')
                player_data['isHomeTeam'] = player_data.get('teamId') == home_team_id
                
                stats = self.parse_player_statistics(player_data, match_id)
                if stats.get('playerId'):
                    statistics_list.append(stats)
        
        # CASO 2: Formato con homeTeam/awayTeam (formato antiguo)
        else:
            for team in ['homeTeam', 'awayTeam']:
                team_players = player_stats.get(team, [])
                if not team_players:
                    continue
                    
                for player in team_players:
                    if not player.get('role'):
                        continue
                        
                    stats = self.parse_player_statistics(player, match_id)
                    if stats.get('playerId'):
                        statistics_list.append(stats)
        
        return statistics_list
    
    def _get_role_from_position(self, position: int) -> str:
        """
        Convierte el código de posición de FotMob a rol legible.
        0 = Goalkeeper, 1 = Defender, 2 = Midfielder, 3 = Attacker
        """
        position_map = {
            0: "Goalkeeper",
            1: "Defender", 
            2: "Midfielder",
            3: "Attacker"
        }
        return position_map.get(position, "Midfielder")
    
    def process_jornada(self, jornada: str, output_file: str = None) -> List[Dict[str, Any]]:
        """
        Procesa todos los partidos de una jornada y genera un JSON.
        
        Args:
            jornada: Nombre de la jornada (ej: 'jornada_1' o '1')
            output_file: Ruta del archivo JSON de salida (opcional)
            
        Returns:
            Lista con todas las estadísticas de la jornada
        """
        matches = self.load_matches()
        
        # Normalizar nombre de jornada
        jornada_key = jornada if jornada.startswith('jornada_') else f'jornada_{jornada}'
        
        if jornada_key not in matches:
            print(f"❌ Error: Jornada '{jornada_key}' no encontrada")
            print(f"   Jornadas disponibles: {', '.join(self.get_available_jornadas())}")
            return []
        
        jornada_matches = matches[jornada_key]
        print(f"\n{'='*70}")
        print(f"🏆 PROCESANDO {jornada_key.upper()}")
        print(f"{'='*70}")
        print(f"📊 {len(jornada_matches)} partidos a procesar\n")
        
        all_statistics = []
        success_count = 0
        
        for i, match in enumerate(jornada_matches, 1):
            match_id = match.get('matchId')
            home_team = match.get('homeTeamId')
            away_team = match.get('awayTeamId')
            score = f"{match.get('homeScore')}-{match.get('awayScore')}"
            
            print(f"[{i}/{len(jornada_matches)}] Partido {match_id} ({home_team} vs {away_team}, {score})")
            
            stats = self.process_match(match_id)
            if stats:
                all_statistics.extend(stats)
                success_count += 1
                print(f"   ✅ {len(stats)} jugadores procesados")
            else:
                print(f"   ⚠️ Sin estadísticas disponibles")
        
        print(f"\n{'='*70}")
        print(f"📈 RESUMEN DE {jornada_key.upper()}")
        print(f"{'='*70}")
        print(f"Partidos procesados: {success_count}/{len(jornada_matches)}")
        print(f"Total jugadores: {len(all_statistics)}")
        
        # Resumen por posición
        positions = {}
        for stat in all_statistics:
            pos = stat.get('playerType', 'UNKNOWN')
            positions[pos] = positions.get(pos, 0) + 1
        
        print(f"\nDistribución por posición:")
        emojis = {'GOALKEEPER': '🧤', 'DEFENDER': '🛡️', 'MIDFIELDER': '⚙️', 'FORWARD': '⚽'}
        for pos in sorted(positions.keys()):
            emoji = emojis.get(pos, '👤')
            print(f"  {emoji} {pos}: {positions[pos]} jugadores")
        
        # Guardar en JSON
        if all_statistics:
            if not output_file:
                output_file = os.path.join(os.path.dirname(__file__), f"{jornada_key}_stats.json")
            
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(all_statistics, f, indent=2, ensure_ascii=False)
            print(f"\n💾 JSON guardado en: {output_file}")
            print(f"📦 {len(all_statistics)} estadísticas listas para enviar al backend")
        
        print(f"{'='*70}\n")
        return all_statistics
    
    def send_bulk_statistics(self, statistics_list: List[Dict[str, Any]]) -> bool:
        """Envía múltiples estadísticas al backend."""
        try:
            url = f"{self.backend_url}/api/statistics/bulk"
            response = requests.post(url, json=statistics_list, timeout=60)
            response.raise_for_status()
            return True
        except requests.RequestException as e:
            print(f"   ✗ Error: {e}")
            return False


# Funciones de conveniencia
def extract_jornada(jornada: str, output_file: str = None):
    """
    Extrae y procesa todas las estadísticas de una jornada, generando un JSON.
    
    Args:
        jornada: Número o nombre de la jornada (ej: '1' o 'jornada_1')
        output_file: Ruta del archivo JSON de salida (opcional)
    
    Returns:
        Lista con todas las estadísticas
    """
    extractor = PlayerStatisticsExtractor()
    return extractor.process_jornada(jornada, output_file)


def list_jornadas():
    """Muestra las jornadas disponibles en matches.json."""
    extractor = PlayerStatisticsExtractor()
    jornadas = extractor.get_available_jornadas()
    
    if not jornadas:
        print("❌ No hay jornadas disponibles")
        print("   Ejecuta primero: python home.py")
        return
    
    print("\n📋 JORNADAS DISPONIBLES EN MATCHES.JSON")
    print("="*50)
    for jornada in jornadas:
        print(f"  • {jornada}")
    print("="*50)
    print(f"\nTotal: {len(jornadas)} jornadas\n")


def check_match(match_id: int):
    """Verifica el estado y disponibilidad de estadísticas de un partido."""
    print(f"\n🔍 DIAGNÓSTICO DEL PARTIDO {match_id}")
    print("="*70)
    
    extractor = PlayerStatisticsExtractor()
    match_data = extractor.get_match_details(match_id)
    
    if not match_data:
        print("❌ No se pudo obtener información del partido")
        return
    
    # Guardar respuesta completa para análisis
    debug_file = os.path.join(os.path.dirname(__file__), f"match_{match_id}_debug.json")
    with open(debug_file, 'w', encoding='utf-8') as f:
        json.dump(match_data, f, indent=2, ensure_ascii=False)
    print(f"💾 Respuesta completa guardada en: {debug_file}")
    
    # Info general
    general = match_data.get('general', {})
    home = general.get('homeTeam', {})
    away = general.get('awayTeam', {})
    status = general.get('status', {})
    
    print(f"\n📊 Partido: {home.get('name', 'N/A')} vs {away.get('name', 'N/A')}")
    print(f"Estado: {'✅ Finalizado' if status.get('finished') else '⏳ No finalizado'}")
    print(f"Resultado: {status.get('scoreStr', 'N/A')}")
    
    # Verificar estadísticas
    content = match_data.get('content', {})
    
    # Mostrar claves disponibles en content
    print(f"\n🔑 Claves en 'content': {list(content.keys())}")
    
    player_stats = content.get('playerStats')
    
    print(f"\n📈 Estadísticas de jugadores:")
    if not player_stats:
        print("❌ No disponibles (campo 'playerStats' no existe)")
        
        # Verificar si hay otras claves relacionadas con jugadores
        possible_keys = [k for k in content.keys() if 'player' in k.lower() or 'lineup' in k.lower() or 'stats' in k.lower()]
        if possible_keys:
            print(f"\n💡 Claves alternativas encontradas: {possible_keys}")
    else:
        home_players = player_stats.get('homeTeam', [])
        away_players = player_stats.get('awayTeam', [])
        print(f"✅ Disponibles")
        print(f"   • Equipo local: {len(home_players)} jugadores")
        print(f"   • Equipo visitante: {len(away_players)} jugadores")
        
        # Contar jugadores con estadísticas válidas
        valid_home = sum(1 for p in home_players if p.get('role'))
        valid_away = sum(1 for p in away_players if p.get('role'))
        print(f"   • Total con rol: {valid_home + valid_away}")
        
        # Mostrar ejemplo de un jugador
        if home_players:
            print(f"\n📝 Ejemplo de jugador (primero del equipo local):")
            example = home_players[0]
            print(f"   • ID: {example.get('id', 'N/A')}")
            print(f"   • Nombre: {example.get('name', {}).get('fullName', 'N/A')}")
            print(f"   • Rol: {example.get('role', 'N/A')}")
            print(f"   • Tiene stats: {'✅' if example.get('stats') else '❌'}")
    
    print("="*70 + "\n")


# Ejemplo de uso
if __name__ == "__main__":
    import sys
    
    print("=" * 70)
    print("EXTRACTOR DE ESTADÍSTICAS POR JORNADA - FotMob API")
    print("=" * 70)
    
    # Si no hay argumentos, mostrar ayuda
    if len(sys.argv) < 2:
        print("\n📖 USO:")
        print("  python players_data.py <jornada> [output_file]")
        print("\nCOMANDOS ESPECIALES:")
        print("  --list                     Ver jornadas disponibles")
        print("  --check <match_id>         Verificar estado de un partido")
        print("\nEJEMPLOS:")
        print("  python players_data.py 1              # Procesar jornada 1 → jornada_1_stats.json")
        print("  python players_data.py 1 custom.json  # Especificar nombre del archivo")
        print("  python players_data.py --list         # Ver jornadas disponibles")
        print("  python players_data.py --check 4837303 # Verificar estado de un partido")
        print()
        list_jornadas()
        sys.exit(0)
    
    # Ver jornadas disponibles
    if sys.argv[1] == '--list':
        list_jornadas()
        sys.exit(0)
    
    # Verificar estado de un partido
    if sys.argv[1] == '--check':
        if len(sys.argv) < 3:
            print("❌ Error: Debes proporcionar un match ID")
            print("   Uso: python players_data.py --check <match_id>")
            sys.exit(1)
        try:
            match_id = int(sys.argv[2])
            check_match(match_id)
        except ValueError:
            print(f"❌ Error: '{sys.argv[2]}' no es un número válido")
        sys.exit(0)
    
    # Procesar jornada
    jornada = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None
    
    stats = extract_jornada(jornada, output_file)
    
    if stats:
        output_name = output_file or f"jornada_{jornada}_stats.json"
        print(f"\n✅ ÉXITO: {len(stats)} estadísticas procesadas")
        print(f"📁 Archivo: {output_name}")
        print(f"💡 Envía al backend con: POST {output_name} → /api/statistics/bulk")
    else:
        print("\n❌ No se encontraron estadísticas para procesar")

