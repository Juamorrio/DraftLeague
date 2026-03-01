import sys

try:
    import pymysql
    conn_module = pymysql
except ImportError:
    try:
        import mysql.connector
        conn_module = mysql.connector
    except ImportError:
        print("Error: Neither pymysql nor mysql.connector is installed")
        print("Install with: pip install pymysql")
        sys.exit(1)

try:
    if conn_module == pymysql:
        conn = pymysql.connect(
            user="root",
            password="juan",
            host="localhost",
            port=3306,
            database="draftleague"
        )
    else:
        conn = conn_module.connect(
            user="root",
            password="juan",
            host="localhost",
            port=3306,
            database="draftleague"
        )

    cursor = conn.cursor()

    # API-Football La Liga 2025-26 team IDs and names
    teams = {
        541: ("Real Madrid", "R. Madrid", "RMA", "https://media.api-sports.io/football/teams/541.png"),
        529: ("Barcelona", "Barcelona", "BAR", "https://media.api-sports.io/football/teams/529.png"),
        530: ("Atletico Madrid", "Atletico", "ATM", "https://media.api-sports.io/football/teams/530.png"),
        548: ("Real Sociedad", "R. Sociedad", "RSO", "https://media.api-sports.io/football/teams/548.png"),
        543: ("Real Betis", "Betis", "BET", "https://media.api-sports.io/football/teams/543.png"),
        531: ("Athletic Club", "Athletic", "ATH", "https://media.api-sports.io/football/teams/531.png"),
        533: ("Villarreal", "Villarreal", "VIL", "https://media.api-sports.io/football/teams/533.png"),
        727: ("Osasuna", "Osasuna", "OSA", "https://media.api-sports.io/football/teams/727.png"),
        547: ("Girona", "Girona", "GIR", "https://media.api-sports.io/football/teams/547.png"),
        798: ("Mallorca", "Mallorca", "MLL", "https://media.api-sports.io/football/teams/798.png"),
        538: ("Celta Vigo", "Celta", "CEL", "https://media.api-sports.io/football/teams/538.png"),
        728: ("Rayo Vallecano", "Rayo", "RAY", "https://media.api-sports.io/football/teams/728.png"),
        546: ("Getafe", "Getafe", "GET", "https://media.api-sports.io/football/teams/546.png"),
        536: ("Sevilla", "Sevilla", "SEV", "https://media.api-sports.io/football/teams/536.png"),
        532: ("Valencia", "Valencia", "VAL", "https://media.api-sports.io/football/teams/532.png"),
        540: ("Espanyol", "Espanyol", "ESP", "https://media.api-sports.io/football/teams/540.png"),
        542: ("Alaves", "Alaves", "ALA", "https://media.api-sports.io/football/teams/542.png"),
        718: ("Real Oviedo", "Oviedo", "OVI", "https://media.api-sports.io/football/teams/718.png"),
        539: ("Levante", "Levante", "LEV", "https://media.api-sports.io/football/teams/539.png"),
        797: ("Elche", "Elche", "ELC", "https://media.api-sports.io/football/teams/797.png"),
    }

    added = 0
    existing = 0

    for team_id, (name, short_name, tla, crest) in teams.items():
        cursor.execute("SELECT id, name FROM football_club WHERE id = %s", (team_id,))
        result = cursor.fetchone()

        if result:
            print(f"  Club {team_id} already exists: {result[1]}")
            existing += 1
        else:
            cursor.execute(
                "INSERT INTO football_club (id, name, short_name, tla, crest) VALUES (%s, %s, %s, %s, %s)",
                (team_id, name, short_name, tla, crest)
            )
            print(f"  Added: {name} (ID: {team_id})")
            added += 1

    conn.commit()

    print(f"\nResumen: {added} equipos nuevos, {existing} ya existentes")

    cursor.close()
    conn.close()

except Exception as e:
    print(f"Error: {e}")
    sys.exit(1)
