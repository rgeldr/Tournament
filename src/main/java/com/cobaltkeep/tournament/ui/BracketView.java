package com.cobaltkeep.tournament.ui;

import com.cobaltkeep.tournament.entity.Match;
import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.service.MatchService;
import com.cobaltkeep.tournament.service.TournamentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Route("bracket")
public class BracketView extends VerticalLayout implements HasUrlParameter<Long> {

    private final TournamentService tournamentService;
    private final MatchService matchService;
    private Long tournamentId;
    private List<List<Player>> rounds = new ArrayList<>();
    private List<Player> losers = new ArrayList<>();
    private Map<String, Match> matchMap = new HashMap<>(); // Key: "roundIndex_player1Id_player2Id" or "losers_player1Id_player2Id"
    private final Button resetButton = new Button("Reset Bracket");
    private Map<String, IntegerField> pointsFields = new HashMap<>(); // Key: "matchId_player1" or "matchId_player2"

    @Autowired
    public BracketView(TournamentService tournamentService, MatchService matchService) {
        this.tournamentService = tournamentService;
        this.matchService = matchService;
        getStyle().set("winner-button", "border: 2px solid green; padding: 5px");
        resetButton.addClickListener(e -> resetBracket());
        add(resetButton);
    }

    @Override
    public void setParameter(BeforeEvent event, Long tournamentId) {
        this.tournamentId = tournamentId;
        Tournament tournament = tournamentService.getTournamentById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        if (tournament.getPlayers().size() < 4 || tournament.getPlayers().size() % 2 != 0) {
            throw new IllegalStateException("Tournament must have at least 4 players and an even number");
        }
        initializeBracket(tournament);
        renderBracket();
    }

    private void initializeBracket(Tournament tournament) {
        rounds.clear();
        losers.clear();
        matchMap.clear();
        pointsFields.clear();

        // Load existing matches
        List<Match> existingMatches = matchService.getMatchesByTournamentAndBracketType(tournament, "main");
        if (existingMatches.isEmpty()) {
            // Initialize first round
            List<Player> randomizedPlayers = new ArrayList<>(tournament.getPlayers());
            Collections.shuffle(randomizedPlayers);
            rounds.add(randomizedPlayers);
            // Create matches for first round
            for (int i = 0; i < randomizedPlayers.size(); i += 2) {
                if (i + 1 < randomizedPlayers.size()) {
                    Player player1 = randomizedPlayers.get(i);
                    Player player2 = randomizedPlayers.get(i + 1);
                    Match match = matchService.createMatch(player1, player2, 0, "main", tournament);
                    matchMap.put("0_" + player1.getId() + "_" + player2.getId(), match);
                }
            }
        } else {
            // Reconstruct rounds from existing matches
            int maxRound = existingMatches.stream().mapToInt(Match::getRound).max().orElse(0);
            for (int roundIndex = 0; roundIndex <= maxRound; roundIndex++) {
                List<Player> round = new ArrayList<>();
                List<Match> roundMatches = matchService.getMatchesByTournamentAndBracketTypeAndRound(tournament, "main", roundIndex);
                for (Match match : roundMatches) {
                    round.add(match.getPlayer1());
                    round.add(match.getPlayer2());
                    matchMap.put(roundIndex + "_" + match.getPlayer1().getId() + "_" + match.getPlayer2().getId(), match);
                    if (match.getWinner() != null && !match.getWinner().equals(match.getPlayer1())) {
                        losers.add(match.getPlayer1());
                    } else if (match.getWinner() != null) {
                        losers.add(match.getPlayer2());
                    }
                }
                if (!round.isEmpty()) {
                    rounds.add(round);
                }
            }
        }

        // Load losers matches
        List<Match> losersMatches = matchService.getMatchesByTournamentAndBracketType(tournament, "losers");
        for (Match match : losersMatches) {
            matchMap.put("losers_" + match.getPlayer1().getId() + "_" + match.getPlayer2().getId(), match);
            if (match.getWinner() != null && !match.getWinner().equals(match.getPlayer1())) {
                losers.remove(match.getPlayer1());
                losers.add(match.getWinner());
            } else if (match.getWinner() != null) {
                losers.remove(match.getPlayer2());
                losers.add(match.getWinner());
            }
        }
    }

    private int getPlayerWins(Player player) {
        return (int) matchService.getMatchesByTournamentAndBracketType(tournamentService.getTournamentById(tournamentId).get(), "main")
                .stream()
                .filter(m -> m.getWinner() != null && m.getWinner().equals(player))
                .count()
                + (int) matchService.getMatchesByTournamentAndBracketType(tournamentService.getTournamentById(tournamentId).get(), "losers")
                .stream()
                .filter(m -> m.getWinner() != null && m.getWinner().equals(player))
                .count();
    }

    private int getPlayerTotalPoints(Player player) {
        return matchService.calculatePlayerStats(tournamentService.getTournamentById(tournamentId).get())
                .getOrDefault(player, new MatchService.PlayerStats(0, 0))
                .getTotalPoints();
    }

    private void renderBracket() {
        removeAll();
        add(new H3("Tournament Bracket"), resetButton);

        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            List<Player> round = rounds.get(roundIndex);
            VerticalLayout roundLayout = new VerticalLayout();
            roundLayout.add(new H3("Round " + (roundIndex + 1)));
            roundLayout.setWidth("400px");

            for (int i = 0; i < round.size(); i += 2) {
                if (i + 1 < round.size()) {
                    Player player1 = round.get(i);
                    Player player2 = round.get(i + 1);
                    String matchKey = roundIndex + "_" + player1.getId() + "_" + player2.getId();
                    Match match = matchMap.get(matchKey);
                    Player winner = match != null ? match.getWinner() : null;

                    HorizontalLayout matchLayout = new HorizontalLayout();
                    matchLayout.setAlignItems(Alignment.CENTER);
                    
                    // Create points input fields
                    IntegerField player1PointsField = new IntegerField();
                    player1PointsField.setLabel("Points");
                    player1PointsField.setWidth("80px");
                    player1PointsField.setValue(match != null ? match.getPlayer1Points() : null);
                    
                    IntegerField player2PointsField = new IntegerField();
                    player2PointsField.setLabel("Points");
                    player2PointsField.setWidth("80px");
                    player2PointsField.setValue(match != null ? match.getPlayer2Points() : null);
                    
                    // Store references to points fields
                    if (match != null) {
                        pointsFields.put(match.getId() + "_player1", player1PointsField);
                        pointsFields.put(match.getId() + "_player2", player2PointsField);
                    }

                    Button player1Button = new Button(player1.getFullName());
                    Button player2Button = new Button(player2.getFullName());
                    Button updatePointsButton = new Button("Update Points", e -> updateMatchPoints(match, player1PointsField, player2PointsField));

                    // Highlight winner
                    if (winner != null) {
                        if (winner.equals(player1)) {
                            player1Button.addClassName("winner-button");
                            player1Button.setEnabled(false);
                            player2Button.setEnabled(false);
                        } else if (winner.equals(player2)) {
                            player2Button.addClassName("winner-button");
                            player1Button.setEnabled(false);
                            player2Button.setEnabled(false);
                        }
                    }

                    matchLayout.add(player1Button, player1PointsField, new Button("vs"), player2PointsField, player2Button, updatePointsButton);
                    roundLayout.add(matchLayout);
                }
            }
            add(roundLayout);
        }

        // Losers matches
        if (!losers.isEmpty()) {
            VerticalLayout losersLayout = new VerticalLayout();
            losersLayout.setWidth("400px");
            losersLayout.add(new H3("Losers Bracket"));

            // Sort losers by wins and points (descending)
            List<Player> sortedLosers = new ArrayList<>(losers);
            sortedLosers.sort((p1, p2) -> {
                int wins1 = getPlayerWins(p1);
                int wins2 = getPlayerWins(p2);
                if (wins1 != wins2) {
                    return Integer.compare(wins2, wins1);
                }
                int points1 = getPlayerTotalPoints(p1);
                int points2 = getPlayerTotalPoints(p2);
                return Integer.compare(points2, points1);
            });

            for (int i = 0; i < sortedLosers.size(); i += 2) {
                if (i + 1 < sortedLosers.size()) {
                    Player player1 = sortedLosers.get(i);
                    Player player2 = sortedLosers.get(i + 1);
                    String matchKey = "losers_" + player1.getId() + "_" + player2.getId();
                    Match match = matchMap.get(matchKey);
                    Player winner = match != null ? match.getWinner() : null;

                    // Create match if it doesn't exist
                    if (match == null) {
                        match = matchService.createMatch(player1, player2, 0, "losers",
                                tournamentService.getTournamentById(tournamentId).get());
                        matchMap.put(matchKey, match);
                    }

                    HorizontalLayout matchLayout = new HorizontalLayout();
                    matchLayout.setAlignItems(Alignment.CENTER);
                    
                    // Create points input fields for losers matches
                    IntegerField player1PointsField = new IntegerField();
                    player1PointsField.setLabel("Points");
                    player1PointsField.setWidth("80px");
                    player1PointsField.setValue(match.getPlayer1Points());
                    
                    IntegerField player2PointsField = new IntegerField();
                    player2PointsField.setLabel("Points");
                    player2PointsField.setWidth("80px");
                    player2PointsField.setValue(match.getPlayer2Points());
                    
                    pointsFields.put(match.getId() + "_player1", player1PointsField);
                    pointsFields.put(match.getId() + "_player2", player2PointsField);

                    Button player1Button = new Button(player1.getFullName() + " (" + getPlayerWins(player1) + " wins, " + getPlayerTotalPoints(player1) + " pts)");
                    Button player2Button = new Button(player2.getFullName() + " (" + getPlayerWins(player2) + " wins, " + getPlayerTotalPoints(player2) + " pts)");
                    Button updatePointsButton = new Button("Update Points", e -> updateMatchPoints(match, player1PointsField, player2PointsField));

                    // Add click handlers for advancing losers
                    player1Button.addClickListener(e -> advanceLoser(player1, player2));
                    player2Button.addClickListener(e -> advanceLoser(player2, player1));

                    // Highlight winner
                    if (winner != null) {
                        if (winner.equals(player1)) {
                            player1Button.addClassName("winner-button");
                            player1Button.setEnabled(false);
                            player2Button.setEnabled(false);
                        } else if (winner.equals(player2)) {
                            player2Button.addClassName("winner-button");
                            player1Button.setEnabled(false);
                            player2Button.setEnabled(false);
                        }
                    }

                    matchLayout.add(player1Button, player1PointsField, new Button("vs"), player2PointsField, player2Button, updatePointsButton);
                    losersLayout.add(matchLayout);
                }
            }
            add(losersLayout);
        }

        // Check for tournament winner
        if (rounds.size() > 1 && rounds.get(rounds.size() - 1).size() == 1) {
            Player winner = rounds.get(rounds.size() - 1).get(0);
            add(new H3("Winner: " + winner.getFullName() + " (" + getPlayerTotalPoints(winner) + " total points)"));
        } else if (rounds.size() > 0 && rounds.get(rounds.size() - 1).size() == 2) {
            // Handle final round with 2 players - determine winner by points
            List<Player> finalRound = rounds.get(rounds.size() - 1);
            Player player1 = finalRound.get(0);
            Player player2 = finalRound.get(1);
            
            int points1 = getPlayerTotalPoints(player1);
            int points2 = getPlayerTotalPoints(player2);
            
            Player finalWinner = points1 >= points2 ? player1 : player2;
            add(new H3("Winner: " + finalWinner.getFullName() + " (" + getPlayerTotalPoints(finalWinner) + " total points)"));
        }

        // Add button to advance to next round based on points
        if (rounds.size() > 0 && matchService.areAllMatchesInRoundCompleted(
                tournamentService.getTournamentById(tournamentId).get(), rounds.size() - 1)) {
            Button advanceRoundButton = new Button("Advance to Next Round", e -> advanceToNextRound());
            add(advanceRoundButton);
        }

        // Add player standings
        addPlayerStandings();
    }

    private void updateMatchPoints(Match match, IntegerField player1PointsField, IntegerField player2PointsField) {
        try {
            Integer player1Points = player1PointsField.getValue();
            Integer player2Points = player2PointsField.getValue();
            
            if (player1Points == null || player2Points == null) {
                Notification.show("Please enter points for both players");
                return;
            }
            
            matchService.updateMatchPoints(match, player1Points, player2Points);
            Notification.show("Points updated successfully");
            renderBracket(); // Refresh the bracket
        } catch (Exception e) {
            Notification.show("Error updating points: " + e.getMessage());
        }
    }

    private void advanceToNextRound() {
        try {
            Tournament tournament = tournamentService.getTournamentById(tournamentId).get();
            int currentRound = rounds.size() - 1;
            
            if (!matchService.areAllMatchesInRoundCompleted(tournament, currentRound)) {
                Notification.show("All matches in the current round must be completed first");
                return;
            }
            
            // Get all players and their stats
            Map<Player, MatchService.PlayerStats> playerStats = matchService.calculatePlayerStats(tournament);
            
            // Sort players by wins (descending), then by points (descending)
            List<Player> sortedPlayers = new ArrayList<>(playerStats.keySet());
            sortedPlayers.sort((p1, p2) -> {
                MatchService.PlayerStats stats1 = playerStats.get(p1);
                MatchService.PlayerStats stats2 = playerStats.get(p2);
                
                if (stats1.getWins() != stats2.getWins()) {
                    return Integer.compare(stats2.getWins(), stats1.getWins());
                }
                return Integer.compare(stats2.getTotalPoints(), stats1.getTotalPoints());
            });
            
            // If we have 2 or fewer players, this is the final round
            if (sortedPlayers.size() <= 2) {
                List<Player> finalRound = new ArrayList<>(sortedPlayers);
                rounds.add(finalRound);
                renderBracket();
                Notification.show("Final round created! Winner will be determined by total points.");
                return;
            }
            
            // Create next round with paired players
            List<Player> nextRound = new ArrayList<>();
            for (int i = 0; i < sortedPlayers.size(); i += 2) {
                if (i + 1 < sortedPlayers.size()) {
                    Player player1 = sortedPlayers.get(i);
                    Player player2 = sortedPlayers.get(i + 1);
                    Match match = matchService.createMatch(player1, player2, currentRound + 1, "main", tournament);
                    matchMap.put((currentRound + 1) + "_" + player1.getId() + "_" + player2.getId(), match);
                    nextRound.add(player1);
                    nextRound.add(player2);
                }
            }
            
            rounds.add(nextRound);
            renderBracket();
            Notification.show("Advanced to Round " + (currentRound + 2));
            
        } catch (Exception e) {
            Notification.show("Error advancing to next round: " + e.getMessage());
        }
    }

    private void advanceLoser(Player winner, Player loser) {
        String matchKey = "losers_" + winner.getId() + "_" + loser.getId();
        Match match = matchMap.get(matchKey);
        if (match != null) {
            matchService.updateMatchWinner(match, winner);
        }
        losers.remove(loser);
        losers.remove(winner);
        losers.add(winner);
        renderBracket();
    }

    private void resetBracket() {
        try {
            Tournament tournament = tournamentService.getTournamentById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
            matchService.deleteMatchesByTournament(tournament);
            tournamentService.unlockTournament(tournament);
            initializeBracket(tournament);
            renderBracket();
            Notification.show("Bracket reset successfully!");
        } catch (Exception e) {
            Notification.show("Error resetting bracket: " + e.getMessage());
        }
    }

    private void addPlayerStandings() {
        try {
            Tournament tournament = tournamentService.getTournamentById(tournamentId).get();
            Map<Player, MatchService.PlayerStats> playerStats = matchService.calculatePlayerStats(tournament);
            
            VerticalLayout standingsLayout = new VerticalLayout();
            standingsLayout.add(new H3("Player Standings"));
            standingsLayout.setWidth("400px");
            
            // Sort players by wins (descending), then by points (descending)
            List<Player> sortedPlayers = new ArrayList<>(playerStats.keySet());
            sortedPlayers.sort((p1, p2) -> {
                MatchService.PlayerStats stats1 = playerStats.get(p1);
                MatchService.PlayerStats stats2 = playerStats.get(p2);
                
                if (stats1.getWins() != stats2.getWins()) {
                    return Integer.compare(stats2.getWins(), stats1.getWins());
                }
                return Integer.compare(stats2.getTotalPoints(), stats1.getTotalPoints());
            });
            
            for (int i = 0; i < sortedPlayers.size(); i++) {
                Player player = sortedPlayers.get(i);
                MatchService.PlayerStats stats = playerStats.get(player);
                String standingText = (i + 1) + ". " + player.getFullName() + 
                                    " - " + stats.getWins() + " wins, " + 
                                    stats.getTotalPoints() + " points";
                standingsLayout.add(standingText);
            }
            
            add(standingsLayout);
        } catch (Exception e) {
            Notification.show("Error loading standings: " + e.getMessage());
        }
    }
}