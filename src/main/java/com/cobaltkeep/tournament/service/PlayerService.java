package com.cobaltkeep.tournament.service;

import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.repository.PlayerRepository;
import com.cobaltkeep.tournament.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Player createPlayer(Player player, Long tournamentId) {
        // Save the player
        Player savedPlayer = playerRepository.save(player);

        // Link to tournament if tournamentId is provided
        if (tournamentId != null) {
            Tournament tournament = tournamentRepository.findById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
            tournament.addPlayer(savedPlayer);
            savedPlayer.addTournament(tournament);
            tournamentRepository.save(tournament);
        }

        return savedPlayer;
    }
}