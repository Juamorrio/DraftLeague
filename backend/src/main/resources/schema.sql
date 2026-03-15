-- ============================================================
-- MIGRATION: Chip system columns (run once on existing databases)
ALTER TABLE team ADD COLUMN IF NOT EXISTS active_chip VARCHAR(50) NULL;
ALTER TABLE team ADD COLUMN IF NOT EXISTS used_chips VARCHAR(500) NULL DEFAULT '';
ALTER TABLE team_gameweek_points ADD COLUMN IF NOT EXISTS applied_chip VARCHAR(50) NULL;
-- ============================================================

-- Tabla de clubs reales de futbol (La Liga)
CREATE TABLE IF NOT EXISTS `football_club` (
    `id` INT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `short_name` VARCHAR(50),
    `tla` VARCHAR(10),
    `crest` VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de partidos
CREATE TABLE IF NOT EXISTS `matches` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `api_football_fixture_id` INT UNIQUE,
    `round` INT,
    `home_team_id` INT,
    `away_team_id` INT,
    `home_club` VARCHAR(255) NOT NULL,
    `away_club` VARCHAR(255) NOT NULL,
    `status` VARCHAR(50) NOT NULL,
    `home_goals` INT DEFAULT 0,
    `away_goals` INT DEFAULT 0,
    `home_xg` DOUBLE,
    `away_xg` DOUBLE,
    `match_date` VARCHAR(255),
    INDEX `idx_api_football_fixture_id` (`api_football_fixture_id`),
    INDEX `idx_round` (`round`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla base para estadisticas detalladas de jugadores por partido (JOINED inheritance)
CREATE TABLE IF NOT EXISTS `player_statistic` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `player_id` VARCHAR(255) NOT NULL,
    `match_id` INT NOT NULL,
    `is_home_team` BOOLEAN NOT NULL,
    `role` VARCHAR(50),
    `player_type` VARCHAR(20) NOT NULL,

    -- Common stats (all players)
    `rating` DOUBLE,
    `minutes_played` INT,
    `accurate_passes` INT,
    `total_passes` INT,
    `duels_won` INT,
    `duels_lost` INT,
    `was_fouled` INT,
    `fouls_committed` INT,
    `yellow_cards` INT DEFAULT 0,
    `red_cards` INT DEFAULT 0,
    `total_fantasy_points` INT DEFAULT 0,

    -- Offensive stats
    `goals` INT,
    `assists` INT,
    `total_shots` INT,
    `shots_on_target` INT,
    `chances_created` INT,
    `successful_dribbles` INT,
    `total_dribbles` INT,
    `dribbled_past` INT,
    `offsides` INT DEFAULT 0,

    -- Passing stats
    `accurate_crosses` INT,
    `total_crosses` INT,

    -- Defensive stats
    `tackles` INT,
    `blocks` INT,
    `interceptions` INT,

    -- Penalty stats
    `penalties_won` INT,
    `penalty_scored` INT DEFAULT 0,
    `penalty_missed` INT DEFAULT 0,

    -- Goalkeeper specific
    `saves` INT,
    `penalties_saved` INT,
    `clean_sheet` BOOLEAN,
    `goals_conceded` INT,

    -- Metadata
    `is_substitute` BOOLEAN DEFAULT FALSE,
    `is_captain` BOOLEAN DEFAULT FALSE,
    `shirt_number` INT,
    `penalty_committed` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY `unique_player_match` (`player_id`, `match_id`),
    INDEX `idx_player_id` (`player_id`),
    INDEX `idx_match_id` (`match_id`),
    INDEX `idx_player_type` (`player_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de puntos por equipo y por jornada
CREATE TABLE IF NOT EXISTS `team_gameweek_points` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `team_id` INT NOT NULL,
    `gameweek` INT NOT NULL,
    `points` INT DEFAULT 0,
    `goalkeeper_points` INT DEFAULT 0,
    `defender_points` INT DEFAULT 0,
    `midfielder_points` INT DEFAULT 0,
    `forward_points` INT DEFAULT 0,
    `captain_id` VARCHAR(255),
    `captain_bonus` INT DEFAULT 0,
    `bench_points` INT DEFAULT 0,
    `top_scorer_id` VARCHAR(255),
    `top_scorer_points` INT,
    `calculated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_team_gameweek` (`team_id`, `gameweek`),
    FOREIGN KEY (`team_id`) REFERENCES `team`(`id`) ON DELETE CASCADE,
    INDEX `idx_gameweek` (`gameweek`),
    INDEX `idx_team_gameweek` (`team_id`, `gameweek`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla para guardar snapshots inmutables de alineaciones por jornada
CREATE TABLE IF NOT EXISTS `team_player_gameweek_points` (
    -- Identificadores
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `team_id` INT NOT NULL,
    `player_id` VARCHAR(255) NOT NULL,
    `gameweek` INT NOT NULL,

    -- Relaciones
    FOREIGN KEY (`team_id`) REFERENCES `team`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`player_id`) REFERENCES `player`(`id`) ON DELETE CASCADE,

    -- Datos del jugador en esta jornada
    `player_name` VARCHAR(255) NOT NULL,
    `position` VARCHAR(10) NOT NULL, -- POR, DEF, MID, DEL
    `points` INT DEFAULT 0, -- Puntos obtenidos (ya con multiplicador de capitan)
    `base_points` INT DEFAULT 0, -- Puntos base sin multiplicador
    `minutes_played` INT DEFAULT 0,
    `match_id` INT,

    -- Estado en el equipo
    `is_in_lineup` BOOLEAN DEFAULT 0,
    `is_captain` BOOLEAN DEFAULT 0,
    `is_benched` BOOLEAN DEFAULT 0,

    -- Timestamps
    `calculated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE KEY `unique_team_player_gameweek` (`team_id`, `player_id`, `gameweek`),
    INDEX `idx_gameweek` (`gameweek`),
    INDEX `idx_team_gameweek` (`team_id`, `gameweek`),
    INDEX `idx_player_gameweek` (`player_id`, `gameweek`),
    INDEX `idx_team_player` (`team_id`, `player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Historial de variaciones del valor de mercado de jugadores por jornada
CREATE TABLE IF NOT EXISTS `player_market_value_history` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `player_id` VARCHAR(255) NOT NULL,
    `gameweek` INT NOT NULL,
    `previous_value` INT NOT NULL,
    `new_value` INT NOT NULL,
    `change_amount` INT NOT NULL,
    `change_percentage` DOUBLE NOT NULL,
    `recorded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_player_gameweek_history` (`player_id`, `gameweek`),
    INDEX `idx_player_history` (`player_id`),
    INDEX `idx_gameweek_history` (`gameweek`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Singleton table that holds the global gameweek state managed by the admin
-- id is always 1 (enforced at application level)
CREATE TABLE IF NOT EXISTS `gameweek_state` (
    `id` INT PRIMARY KEY DEFAULT 1,
    `active_gameweek` INT,
    `teams_locked` BOOLEAN NOT NULL DEFAULT FALSE,
    `locked_at` TIMESTAMP NULL,
    `unlocked_at` TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
