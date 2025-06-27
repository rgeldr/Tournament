package com.cobaltkeep.tournament.service;

import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.repository.PlayerRepository;
import com.cobaltkeep.tournament.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TournamentService {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private PlayerRepository playerRepository;

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> getTournamentById(Long id) {
        return tournamentRepository.findByIdWithPlayers(id);
    }

    @Transactional
    public Tournament createTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    @Transactional
    public Tournament updateTournament(Long id, Tournament tournamentDetails) {
        Tournament tournament = tournamentRepository.findByIdWithPlayers(id).orElseThrow();
        tournament.setName(tournamentDetails.getName());
        tournament.setStartDate(tournamentDetails.getStartDate());
        tournament.setEndDate(tournamentDetails.getEndDate());
        tournament.setLocked(tournamentDetails.isLocked());
        // Preserve players if they exist
        if (tournamentDetails.getPlayers() != null && !tournamentDetails.getPlayers().isEmpty()) {
            tournament.setPlayers(tournamentDetails.getPlayers());
        }
        return tournamentRepository.save(tournament);
    }

    public void deleteTournament(Long id) {
        tournamentRepository.deleteById(id);
    }

    @Transactional
    public void unlockTournament(Tournament tournament) {
        tournament.setLocked(false);
        tournamentRepository.save(tournament);
    }

    @Transactional
    public void addPlayersToTournament(Long tournamentId, List<Long> playerIds) {
        Tournament tournament = tournamentRepository.findByIdWithPlayers(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        List<Player> players = playerRepository.findAllById(playerIds);
        tournament.getPlayers().addAll(players);
        tournamentRepository.save(tournament);
    }
}