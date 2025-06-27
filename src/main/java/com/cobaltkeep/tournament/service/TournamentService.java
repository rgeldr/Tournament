package com.cobaltkeep.tournament.service;

import com.cobaltkeep.tournament.entity.Tournament;
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
    public void startTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findByIdWithPlayers(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        
        if (tournament.getPlayers().size() < 4) {
            throw new IllegalStateException("Tournament must have at least 4 players to start");
        }
        
        if (tournament.getPlayers().size() % 2 != 0) {
            throw new IllegalStateException("Tournament must have an even number of players to start");
        }
        
        tournament.setLocked(true);
        tournamentRepository.save(tournament);
    }
}