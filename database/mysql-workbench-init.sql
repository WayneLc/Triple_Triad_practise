-- Triple Triad MySQL 8 initialization script
CREATE DATABASE IF NOT EXISTS triple_triad;
USE triple_triad;

CREATE TABLE IF NOT EXISTS players (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  tutorial_seen BIT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_players_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cards (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  stars INT NOT NULL,
  top_value INT NOT NULL,
  right_value INT NOT NULL,
  bottom_value INT NOT NULL,
  left_value INT NOT NULL,
  element VARCHAR(255),
  PRIMARY KEY (id),
  UNIQUE KEY uk_cards_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS decks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  player_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  card_ids VARCHAR(200) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_decks_player FOREIGN KEY (player_id) REFERENCES players(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rooms (
  id BIGINT NOT NULL AUTO_INCREMENT,
  host_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  rule_name VARCHAR(255),
  password VARCHAR(255),
  turn_seconds INT NOT NULL,
  created_at DATETIME(6),
  expires_at DATETIME(6),
  guest_id BIGINT,
  host_deck_id VARCHAR(255), guest_deck_id VARCHAR(255),
  host_ready BIT NOT NULL DEFAULT 0, guest_ready BIT NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
  disconnected_at DATETIME(6), game_state TEXT,
  PRIMARY KEY (id),
  CONSTRAINT fk_rooms_host FOREIGN KEY (host_id) REFERENCES players(id),
  CONSTRAINT fk_rooms_guest FOREIGN KEY (guest_id) REFERENCES players(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS battles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  player_one_id BIGINT,
  player_two_id BIGINT,
  rule_name VARCHAR(255),
  board_state VARCHAR(255),
  started_at DATETIME(6),
  finished_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
