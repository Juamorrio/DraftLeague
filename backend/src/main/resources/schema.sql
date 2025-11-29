
ALTER TABLE `leagues` MODIFY COLUMN `chat_id` INT NULL;
ALTER TABLE `leagues` MODIFY COLUMN `notification_league_id` INT NULL;
ALTER TABLE `team` MODIFY COLUMN `player_team_id` INT NULL;

UPDATE player SET position='POR' WHERE position IN ('GK');
UPDATE player SET position='DEF' WHERE position IN ('LB','RB','CB');
UPDATE player SET position='MID' WHERE position IN ('CM','CAM');
UPDATE player SET position='DEL' WHERE position IN ('LW','RW','ST');

INSERT INTO player_statistic (minutes_played, goals, assists, shots, shots_on_target, fouls_drawn, fouls_committed, own_goals, yellow_cards, red_cards, rating_avg, total_fantasy_points)
VALUES (0,0,0,0,0,0,0,0,0,0,0,0);
INSERT INTO player_score (minutes_played, goals, assists, shots, shots_on_target, key_passes, dribbles_completed, fouls_drawn, fouls_committed, big_chances_created, duels_won, tackles, interceptions, clearances, blocks, own_goals, penalties_saved, saves, goals_conceded, clean_sheet, yellow_cards, red_cards)
VALUES (0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,false,0,0);


INSERT INTO player (id, full_name, position, market_value, active, total_points, avatar_url, player_score_id, player_statistic_id) VALUES
 (1, 'Juan Pérez', 'POR', 5000000, true, 0, null, 1, 1),
 (2, 'Luis García', 'DEF', 3000000, true, 0, null, 1, 1),
 (3, 'Carlos Ruiz', 'DEF', 4000000, true, 0, null, 1, 1),
 (4, 'Miguel López', 'DEF', 3500000, true, 0, null, 1, 1),
 (5, 'Pedro Díaz', 'DEF', 3000000, true, 0, null, 1, 1),
 (6, 'Andrés Soto', 'MID', 4200000, true, 0, null, 1, 1),
 (7, 'Sergio León', 'MID', 4000000, true, 0, null, 1, 1),
 (8, 'Álvaro Rey', 'DEL', 4800000, true, 0, null, 1, 1),
 (9, 'David Navas', 'MID', 5100000, true, 0, null, 1, 1),
 (10, 'Raúl Vega', 'DEL', 4900000, true, 0, null, 1, 1),
 (11, 'Antony', 'DEL', 6000000, true, 0, 'frontend\assets\Player\antony.png', 1, 1);