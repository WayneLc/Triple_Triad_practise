package com.tripletriad.model;

import jakarta.persistence.*;

@Entity @Table(name = "decks")
public class Deck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Player player;
    @Column(nullable = false) public String name;
    @Column(nullable = false, length = 200) public String cardIds;
    public Deck() {}
}
