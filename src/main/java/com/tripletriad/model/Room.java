package com.tripletriad.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "rooms")
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Player host;
    @Column(nullable = false) public String title;
    public String ruleName = "STANDARD";
    public String password;
    public int turnSeconds = 60;
    public Instant createdAt = Instant.now();
    public Room() {}
}
