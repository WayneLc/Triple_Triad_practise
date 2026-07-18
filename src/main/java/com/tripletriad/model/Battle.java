package com.tripletriad.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "battles")
public class Battle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long playerOneId; public Long playerTwoId;
    public String ruleName; public String boardState;
    public Instant startedAt = Instant.now(); public Instant finishedAt;
    protected Battle() {}
}
