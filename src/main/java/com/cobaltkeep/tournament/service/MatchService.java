package com.cobaltkeep.tournament.service;

import com.cobaltkeep.tournament.entity.Match;
import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    public Match createMatch(Player player1, Player player2, int round, String bracketType, Tournament tournament) {
        Match match = new Match(player1, player2, null, round, bracketType, tournament);
        return matchRepository.save(match);
    }

    public Match updateMatchWinner(Match match, Player winner) {
        match.setWinner(winner);
        return matchRepository.save(match);
    }

    public List<Match> getMatchesByTournamentAndBracketType(Tournament tournament, String bracketType) {
        return matchRepository.findByTournamentAndBracketType(tournament, bracketType);
    }

    public List<Match> getMatchesByTournamentAndBracketTypeAndRound(Tournament tournament, String bracketType, int round) {
        return matchRepository.findByTournamentAndBracketTypeAndRound(tournament, bracketType, round);
    }
}