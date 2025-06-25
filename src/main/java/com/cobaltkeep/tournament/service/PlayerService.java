package com.cobaltkeep.tournament.service;

import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.repository.PlayerRepository;
import com.cobaltkeep.tournament.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public Player createPlayer(Player player, Long tournamentId) {
        // Check for duplicate email
        if (playerRepository.findByEmail(player.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Save the player
        Player savedPlayer = playerRepository.save(player);

        // Link to tournament if tournamentId is provided
        if (tournamentId != null) {
            Tournament tournament = tournamentRepository.findByIdWithPlayers(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
            if (tournament.isLocked()) {
                throw new IllegalStateException("Tournament is locked, cannot add players");
            }
            tournament.addPlayer(savedPlayer);
            savedPlayer.addTournament(tournament);
            tournamentRepository.save(tournament);
        }

        return savedPlayer;
    }

    public Optional<Player> findByEmail(String email) {
        return playerRepository.findByEmail(email);
    }
}