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
    public Instant expiresAt = Instant.now().plusSeconds(240);
    @ManyToOne public Player guest;
    public String hostDeckId;
    public String guestDeckId;
    public boolean hostReady;
    public boolean guestReady;
    public String hostNextChoice;
    public String guestNextChoice;
    public Instant hostLastSeen = Instant.now();
    public Instant guestLastSeen;
    public String status = "WAITING"; // WAITING, PLAYING, DISCONNECTED
    public Instant disconnectedAt;
    /** A short-lived notice shown to the player who remains in the room. */
    public String departureRole; // HOST, GUEST
    public String departureReason; // LEFT, DISCONNECTED
    public String departurePhase; // POST_GAME, ROOM
    public Instant departureAt;
    public Instant departureUntil;
    @Column(length = 12000) public String gameState;
    public Room() {}
}
