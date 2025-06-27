package com.cobaltkeep.tournament.service;

import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.repository.PlayerRepository;
import com.cobaltkeep.tournament.repository.TournamentRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;

    // Constructor injection (assuming repositories are autowired)
    public PlayerService(PlayerRepository playerRepository, TournamentRepository tournamentRepository) {
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public List<Player> getAvailablePlayers(Long tournamentId) {
        return playerRepository.findAvailablePlayers(tournamentId);
    }

    public Player createPlayer(Player player, Long tournamentId) {
        // Check for duplicate firstName and lastName combination
        if (playerRepository.findByFirstNameAndLastName(player.getFirstName(), player.getLastName()).isPresent()) {
            throw new IllegalArgumentException("Player with name " + player.getFirstName() + " " + player.getLastName() + " already exists");
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

    @Transactional
    public void addPlayerToTournament(Long playerId, Long tournamentId) {
        // Fetch the player by ID
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found with ID: " + playerId));

        // Fetch the tournament by ID
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found with ID: " + tournamentId));

        // Access the tournaments collection and add the tournament if it’s not already present
        if (!player.getTournaments().contains(tournament)) {
            player.getTournaments().add(tournament);
            playerRepository.save(player); // Persist the changes
        }
    }

}