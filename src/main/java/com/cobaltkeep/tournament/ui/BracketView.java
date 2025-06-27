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
                    int finalRoundIndex = roundIndex;
                    Button winner1Button = new Button(player1.getFullName(), e -> advancePlayer(player1, player2, finalRoundIndex));
                    int finalRoundIndex1 = roundIndex;
                    Button winner2Button = new Button(player2.getFullName(), e -> advancePlayer(player2, player1, finalRoundIndex1));

                    // Highlight winner
                    if (winner != null) {
                        if (winner.equals(player1)) {
                            winner1Button.addClassName("winner-button");
                            winner1Button.setEnabled(false);
                            winner2Button.setEnabled(false);
                        } else if (winner.equals(player2)) {
                            winner2Button.addClassName("winner-button");
                            winner1Button.setEnabled(false);
                            winner2Button.setEnabled(false);
                        }
                    }

                    matchLayout.add(winner1Button, new Button("vs"), winner2Button);
                    roundLayout.add(matchLayout);
                }
            }
            add(roundLayout);
        }

        // Losers matches
        if (!losers.isEmpty()) {
            VerticalLayout losersLayout = new VerticalLayout();
            losersLayout.setWidth("400px");

            // Sort losers by wins (descending)
            List<Player> sortedLosers = new ArrayList<>(losers);
            sortedLosers.sort((p1, p2) -> Integer.compare(getPlayerWins(p2), getPlayerWins(p1)));

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
                    Button winner1Button = new Button(player1.getFullName() + " (" + getPlayerWins(player1) + " wins)",
                            e -> advanceLoser(player1, player2));
                    Button winner2Button = new Button(player2.getFullName() + " (" + getPlayerWins(player2) + " wins)",
                            e -> advanceLoser(player2, player1));

                    // Highlight winner
                    if (winner != null) {
                        if (winner.equals(player1)) {
                            winner1Button.addClassName("winner-button");
                            winner1Button.setEnabled(false);
                            winner2Button.setEnabled(false);
                        } else if (winner.equals(player2)) {
                            winner2Button.addClassName("winner-button");
                            winner1Button.setEnabled(false);
                            winner2Button.setEnabled(false);
                        }
                    }

                    matchLayout.add(winner1Button, new Button("vs"), winner2Button);
                    losersLayout.add(matchLayout);
                }
            }
            add(losersLayout);
        }

        // Check for tournament winner
        if (rounds.size() > 1 && rounds.get(rounds.size() - 1).size() == 1) {
            add(new H3("Winner: " + rounds.get(rounds.size() - 1).get(0).getFullName()));
        }
    }

    private void advancePlayer(Player winner, Player loser, int roundIndex) {
        String matchKey = roundIndex + "_" + winner.getId() + "_" + loser.getId();
        Match match = matchMap.get(matchKey);
        if (match != null) {
            matchService.updateMatchWinner(match, winner);
        }
        losers.add(loser);

        // Check if all matches in the current round are complete
        List<Player> currentRound = rounds.get(roundIndex);
        boolean allMatchesComplete = true;
        int expectedPairs = currentRound.size() / 2;
        int completedPairs = 0;
        for (int i = 0; i < currentRound.size(); i += 2) {
            if (i + 1 < currentRound.size()) {
                String key = roundIndex + "_" + currentRound.get(i).getId() + "_" + currentRound.get(i + 1).getId();
                match = matchMap.get(key);
                if (match != null && match.getWinner() != null) {
                    completedPairs++;
                } else {
                    allMatchesComplete = false;
                    break;
                }
            }
        }
        allMatchesComplete = (completedPairs == expectedPairs);

        if (allMatchesComplete) {
            int nextRoundIndex = roundIndex + 1;
            List<Player> nextRound = new ArrayList<>();
            if (nextRoundIndex < rounds.size()) {
                nextRound = rounds.get(nextRoundIndex);
            } else {
                rounds.add(nextRound);
            }
            // Add winners to the next round
            for (int i = 0; i < currentRound.size(); i += 2) {
                if (i + 1 < currentRound.size()) {
                    String key = roundIndex + "_" + currentRound.get(i).getId() + "_" + currentRound.get(i + 1).getId();
                    match = matchMap.get(key);
                    if (match != null && match.getWinner() != null) {
                        nextRound.add(match.getWinner());
                    }
                }
            }
            // Create matches for next round
            if (!nextRound.isEmpty() && nextRound.size() >= 2) {
                Collections.shuffle(nextRound);
                for (int i = 0; i < nextRound.size(); i += 2) {
                    if (i + 1 < nextRound.size()) {
                        Player player1 = nextRound.get(i);
                        Player player2 = nextRound.get(i + 1);
                        match = matchService.createMatch(player1, player2, nextRoundIndex, "main",
                                tournamentService.getTournamentById(tournamentId).get());
                        matchMap.put(nextRoundIndex + "_" + player1.getId() + "_" + player2.getId(), match);
                    }
                }
            }
        }

        renderBracket();
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
}