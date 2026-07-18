package com.tripletriad.model;

import jakarta.persistence.*;

@Entity @Table(name = "cards")
public class Card {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false) public String name;
    public int stars, topValue, rightValue, bottomValue, leftValue;
    public String element;
    protected Card() {}
    public Card(String name, int stars, int t, int r, int b, int l, String element) { this.name=name; this.stars=stars; topValue=t; rightValue=r; bottomValue=b; leftValue=l; this.element=element; }
}
