package com.cobaltkeep.tournament.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private Player player1;

    @ManyToOne
    @JoinColumn(name = "player2_id", nullable = false)
    private Player player2;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Player winner;

    @Column(name = "round", nullable = false)
    private int round;

    @Column(name = "bracket_type", nullable = false)
    private String bracketType; // "main" or "losers"

    @ManyToOne
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "player1_points")
    private Integer player1Points;

    @Column(name = "player2_points")
    private Integer player2Points;

    // No-arg constructor
    public Match() {}

    // Constructor with fields
    public Match(Player player1, Player player2, Player winner, int round, String bracketType, Tournament tournament) {
        this.player1 = player1;
        this.player2 = player2;
        this.winner = winner;
        this.round = round;
        this.bracketType = bracketType;
        this.tournament = tournament;
        this.player1Points = null;
        this.player2Points = null;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Player getPlayer1() { return player1; }
    public void setPlayer1(Player player1) { this.player1 = player1; }
    public Player getPlayer2() { return player2; }
    public void setPlayer2(Player player2) { this.player2 = player2; }
    public Player getWinner() { return winner; }
    public void setWinner(Player winner) { this.winner = winner; }
    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }
    public String getBracketType() { return bracketType; }
    public void setBracketType(String bracketType) { this.bracketType = bracketType; }
    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }
    public Integer getPlayer1Points() { return player1Points; }
    public void setPlayer1Points(Integer player1Points) { this.player1Points = player1Points; }
    public Integer getPlayer2Points() { return player2Points; }
    public void setPlayer2Points(Integer player2Points) { this.player2Points = player2Points; }
}