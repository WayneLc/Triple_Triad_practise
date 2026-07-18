package com.tripletriad.model;

import jakarta.persistence.*;

@Entity @Table(name = "players")
public class Player {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false) public String username;
    @Column(nullable = false) public String password;
    public boolean tutorialSeen;
    protected Player() {}
    public Player(String username, String password) { this.username = username; this.password = password; }
}
