package com.cobaltkeep.tournament.repository;

import com.cobaltkeep.tournament.entity.Match;
import com.cobaltkeep.tournament.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournamentAndBracketType(Tournament tournament, String bracketType);
    List<Match> findByTournamentAndBracketTypeAndRound(Tournament tournament, String bracketType, int round);
    void deleteByTournament(Tournament tournament);
    List<Match> findByTournament(Tournament tournament);
}