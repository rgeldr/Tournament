package com.cobaltkeep.tournament.service;

import com.cobaltkeep.tournament.entity.Match;
import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    @Transactional
    public Match createMatch(Player player1, Player player2, int round, String bracketType, Tournament tournament) {
        Match match = new Match(player1, player2, null, round, bracketType, tournament);
        return matchRepository.save(match);
    }

    @Transactional
    public Match updateMatchWinner(Match match, Player winner) {
        match.setWinner(winner);
        return matchRepository.save(match);
    }

    @Transactional
    public Match updateMatchPoints(Match match, Integer player1Points, Integer player2Points) {
        match.setPlayer1Points(player1Points);
        match.setPlayer2Points(player2Points);
        
        // Determine winner based on points
        if (player1Points != null && player2Points != null) {
            if (player1Points > player2Points) {
                match.setWinner(match.getPlayer1());
            } else if (player2Points > player1Points) {
                match.setWinner(match.getPlayer2());
            }
            // If points are equal, winner remains null (tie)
        }
        
        return matchRepository.save(match);
    }

    public List<Match> getMatchesByTournamentAndBracketType(Tournament tournament, String bracketType) {
        return matchRepository.findByTournamentAndBracketType(tournament, bracketType);
    }

    public List<Match> getMatchesByTournamentAndBracketTypeAndRound(Tournament tournament, String bracketType, int round) {
        return matchRepository.findByTournamentAndBracketTypeAndRound(tournament, bracketType, round);
    }

    @Transactional
    public void deleteMatchesByTournament(Tournament tournament) {
        matchRepository.deleteByTournament(tournament);
    }

    /**
     * Calculate player statistics including wins and total points
     */
    public Map<Player, PlayerStats> calculatePlayerStats(Tournament tournament) {
        List<Match> allMatches = matchRepository.findByTournament(tournament);
        
        return allMatches.stream()
                .flatMap(match -> List.of(match.getPlayer1(), match.getPlayer2()).stream())
                .distinct()
                .collect(Collectors.toMap(
                        player -> player,
                        player -> {
                            int wins = (int) allMatches.stream()
                                    .filter(m -> m.getWinner() != null && m.getWinner().equals(player))
                                    .count();
                            
                            int totalPoints = allMatches.stream()
                                    .filter(m -> m.getPlayer1().equals(player) || m.getPlayer2().equals(player))
                                    .mapToInt(m -> {
                                        if (m.getPlayer1().equals(player) && m.getPlayer1Points() != null) {
                                            return m.getPlayer1Points();
                                        } else if (m.getPlayer2().equals(player) && m.getPlayer2Points() != null) {
                                            return m.getPlayer2Points();
                                        }
                                        return 0;
                                    })
                                    .sum();
                            
                            return new PlayerStats(wins, totalPoints);
                        }
                ));
    }

    /**
     * Get all matches for a tournament
     */
    public List<Match> getMatchesByTournament(Tournament tournament) {
        return matchRepository.findByTournament(tournament);
    }

    /**
     * Check if all matches in a round are completed (have points)
     */
    public boolean areAllMatchesInRoundCompleted(Tournament tournament, int round) {
        List<Match> roundMatches = getMatchesByTournamentAndBracketTypeAndRound(tournament, "main", round);
        return roundMatches.stream().allMatch(match -> 
                match.getPlayer1Points() != null && match.getPlayer2Points() != null);
    }

    /**
     * Inner class to hold player statistics
     */
    public static class PlayerStats {
        private final int wins;
        private final int totalPoints;

        public PlayerStats(int wins, int totalPoints) {
            this.wins = wins;
            this.totalPoints = totalPoints;
        }

        public int getWins() { return wins; }
        public int getTotalPoints() { return totalPoints; }
    }
}