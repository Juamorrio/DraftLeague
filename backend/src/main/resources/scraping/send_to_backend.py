import requests
import json
import os

def send_statistics_to_backend(json_file: str, backend_url: str = "http://localhost:8080"):
    """
    Envía estadísticas de jugadores al backend desde un archivo JSON.
    
    Args:
        json_file: Ruta al archivo JSON con las estadísticas
        backend_url: URL base del backend
    """
    if not os.path.exists(json_file):
        print(f"ERROR: Archivo {json_file} no encontrado")
        return False
    
    print(f"\n{'='*70}")
    print(f"ENVIANDO ESTADISTICAS AL BACKEND")
    print(f"{'='*70}")
    print(f"Archivo: {json_file}")
    print(f"Backend: {backend_url}")
    
    try:
        with open(json_file, 'r', encoding='utf-8') as f:
            statistics = json.load(f)
        
        print(f"\nEstadisticas cargadas: {len(statistics)}")
        
        url = f"{backend_url}/api/statistics/bulk"
        print(f"Enviando a: {url}")
        
        response = requests.post(
            url,
            json=statistics,
            headers={'Content-Type': 'application/json'},
            timeout=120
        )
        
        response.raise_for_status()
        result = response.json()
        
        print(f"\n{'='*70}")
        print(f"RESULTADO")
        print(f"{'='*70}")
        print(f"Status: {response.status_code}")
        print(f"Mensaje: {result.get('message', 'N/A')}")
        print(f"Registros guardados: {result.get('count', 'N/A')}")
        print(f"{'='*70}\n")
        
        return True
        
    except requests.RequestException as e:
        print(f"\nERROR en la peticion HTTP:")
        print(f"  {e}")
        if hasattr(e, 'response') and e.response is not None:
            print(f"  Status code: {e.response.status_code}")
            try:
                error_data = e.response.json()
                print(f"  Error del servidor: {error_data}")
            except:
                print(f"  Respuesta: {e.response.text[:200]}")
        return False
        
    except json.JSONDecodeError as e:
        print(f"\nERROR leyendo JSON:")
        print(f"  {e}")
        return False
        
    except Exception as e:
        print(f"\nERROR inesperado:")
        print(f"  {e}")
        return False


def send_all_jornadas(backend_url: str = "http://localhost:8080"):
    """
    Envía todas las jornadas encontradas en el directorio actual.
    """
    script_dir = os.path.dirname(__file__)
    json_files = [f for f in os.listdir(script_dir) if f.startswith('jornada_') and f.endswith('_stats.json')]
    
    if not json_files:
        print("No se encontraron archivos de jornadas (jornada_*_stats.json)")
        return
    
    print(f"\nArchivos encontrados: {len(json_files)}")
    for f in sorted(json_files):
        print(f"  - {f}")
    
    print(f"\n{'='*70}")
    success_count = 0
    
    for json_file in sorted(json_files):
        file_path = os.path.join(script_dir, json_file)
        if send_statistics_to_backend(file_path, backend_url):
            success_count += 1
        print()
    
    print(f"{'='*70}")
    print(f"RESUMEN: {success_count}/{len(json_files)} jornadas enviadas correctamente")
    print(f"{'='*70}\n")


if __name__ == "__main__":
    import sys
    
    print("=" * 70)
    print("ENVIO DE ESTADISTICAS AL BACKEND")
    print("=" * 70)
    
    if len(sys.argv) < 2:
        print("\nUSO:")
        print("  python send_to_backend.py <archivo_json>")
        print("  python send_to_backend.py --all")
        print("\nEJEMPLOS:")
        print("  python send_to_backend.py jornada_22_stats.json")
        print("  python send_to_backend.py --all")
        print()
        sys.exit(0)
    
    if sys.argv[1] == '--all':
        send_all_jornadas()
    else:
        json_file = sys.argv[1]
        send_statistics_to_backend(json_file)
