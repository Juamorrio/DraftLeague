
ALTER TABLE `leagues` MODIFY COLUMN `chat_id` INT NULL;
ALTER TABLE `leagues` MODIFY COLUMN `notification_league_id` INT NULL;
ALTER TABLE `team` MODIFY COLUMN `player_team_id` INT NULL;
ALTER TABLE player_team DROP COLUMN IF EXISTS player_league_id;
DROP TABLE IF EXISTS `player_league`;
ALTER TABLE `leagues` ADD COLUMN IF NOT EXISTS `created_by_user_id` INT NULL;
ALTER TABLE `leagues` ADD CONSTRAINT `fk_league_created_by` FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL;
ALTER TABLE `market_player` ADD COLUMN IF NOT EXISTS `bids` TEXT NULL;

-- Limpiar referencias huérfanas ANTES de cualquier operación
DELETE FROM player_team WHERE player_id NOT IN (SELECT id FROM player);
DELETE FROM market_player WHERE player_id NOT IN (SELECT id FROM player);

ALTER TABLE player MODIFY COLUMN position VARCHAR(10) NOT NULL;

UPDATE player SET position='POR' WHERE position IN ('GK');
UPDATE player SET position='DEF' WHERE position IN ('LB','RB','CB');
UPDATE player SET position='MID' WHERE position IN ('CM','CAM');
UPDATE player SET position='DEL' WHERE position IN ('LW','RW','ST');

INSERT INTO player_statistic (minutes_played, goals, assists, shots, shots_on_target, fouls_drawn, fouls_committed, own_goals, yellow_cards, red_cards, rating_avg, total_fantasy_points)
VALUES (0,0,0,0,0,0,0,0,0,0,0,0);
INSERT INTO player_score (minutes_played, goals, assists, shots, shots_on_target, key_passes, dribbles_completed, fouls_drawn, fouls_committed, big_chances_created, duels_won, tackles, interceptions, clearances, blocks, own_goals, penalties_saved, saves, goals_conceded, clean_sheet, yellow_cards, red_cards)
VALUES (0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,false,0,0);

ALTER TABLE player MODIFY COLUMN player_score_id INT NULL;

-- Asegurar integridad referencial para evitar nuevos huérfanos
ALTER TABLE player_team
	ADD CONSTRAINT fk_playerteam_player FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE;
ALTER TABLE market_player
	ADD CONSTRAINT fk_marketplayer_player FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE;