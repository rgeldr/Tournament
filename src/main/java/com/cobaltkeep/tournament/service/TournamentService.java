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
public class TournamentService {

    @Autowired
    private TournamentRepository tournamentRepository;

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> getTournamentById(Long id) {
        return tournamentRepository.findByIdWithPlayers(id);
    }

    public Tournament createTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    @Autowired
    private PlayerRepository playerRepository;

    public Tournament updateTournament(Long id, Tournament tournamentDetails) {
        Tournament tournament = tournamentRepository.findById(id).orElseThrow();
        tournament.setName(tournamentDetails.getName());
        tournament.setStartDate(tournamentDetails.getStartDate());
        tournament.setEndDate(tournamentDetails.getEndDate());
        return tournamentRepository.save(tournament);
    }

    public void deleteTournament(Long id) {
        tournamentRepository.deleteById(id);
    }

    public void unlockTournament(Tournament tournament) {
    }

    public void addPlayersToTournament(Long tournamentId, List<Long> playerIds) {
        Tournament tournament = tournamentRepository.findByIdWithPlayers(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        List<Player> players = playerRepository.findAllById(playerIds); // Error occurs here
        tournament.getPlayers().addAll(players);
        tournamentRepository.save(tournament);
    }
}