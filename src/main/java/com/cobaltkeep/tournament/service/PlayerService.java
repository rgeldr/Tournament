package com.cobaltkeep.tournament.service;

import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.repository.PlayerRepository;
import com.cobaltkeep.tournament.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
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

        // Fetch the tournament by ID with players
        Tournament tournament = tournamentRepository.findByIdWithPlayers(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found with ID: " + tournamentId));

        if (tournament.isLocked()) {
            throw new IllegalStateException("Tournament is locked, cannot add players");
        }

        // Add the player to the tournament (bidirectional relationship)
        if (!tournament.getPlayers().contains(player)) {
            tournament.addPlayer(player);
            player.addTournament(tournament);
            tournamentRepository.save(tournament);
        }
    }

    @Transactional
    public void removePlayerFromTournament(Long playerId, Long tournamentId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        Tournament tournament = tournamentRepository.findByIdWithPlayers(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        
        if (tournament.isLocked()) {
            throw new IllegalStateException("Tournament is locked, cannot remove players");
        }
        
        tournament.getPlayers().remove(player);
        player.getTournaments().remove(tournament);
        tournamentRepository.save(tournament);
    }
}