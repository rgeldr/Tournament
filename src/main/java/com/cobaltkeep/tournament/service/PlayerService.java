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

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
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