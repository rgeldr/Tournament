package com.cobaltkeep.tournament.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true)
    private String email;


    @ManyToMany(mappedBy = "players")
    private Set<Tournament> tournaments = new HashSet<>();

    // No-arg constructor
    public Player() {}

    // Constructor with fields
    public Player(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<Tournament> getTournaments() { return tournaments; }
    public void setTournaments(Set<Tournament> tournaments) { this.tournaments = tournaments; }
    public void addTournament(Tournament tournament) { this.tournaments.add(tournament); }
}