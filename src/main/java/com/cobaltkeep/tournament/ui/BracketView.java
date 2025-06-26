package com.cobaltkeep.tournament.ui;

import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.service.TournamentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
    private Long tournamentId;
    private List<List<Player>> rounds = new ArrayList<>();
    private List<Player> losers = new ArrayList<>();
    private Map<String, Player> matchResults = new HashMap<>(); // Key: "roundIndex_player1Id_player2Id" or "losers_player1Id_player2Id", Value: winner

    @Autowired
    public BracketView(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
        // Add CSS for winner highlight
        getStyle().set("winner-button", "border: 2px solid green; padding: 5px");
    }

    @Override
    public void setParameter(BeforeEvent event, Long tournamentId) {
        this.tournamentId = tournamentId;
        Tournament tournament = tournamentService.getTournamentById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        if (tournament.getPlayers().size() < 4 || tournament.getPlayers().size() % 2 != 0) {
            throw new IllegalStateException("Tournament must have at least 4 players and an even number");
        }
        initializeBracket(new ArrayList<>(tournament.getPlayers()));
        renderBracket();
    }

    private void initializeBracket(List<Player> players) {
        rounds.clear();
        losers.clear();
        matchResults.clear();
        List<Player> randomizedPlayers = new ArrayList<>(players);
        Collections.shuffle(randomizedPlayers);
        rounds.add(randomizedPlayers);
    }

    private int getPlayerWins(Player player) {
        return (int) matchResults.values().stream()
                .filter(winner -> winner.equals(player))
                .count();
    }

    private void renderBracket() {
        removeAll();
        add(new H3("Tournament Bracket"));

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
                    Player winner = matchResults.get(matchKey);

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

        // Losers matches (without "Losers Bracket" title)
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
                    Player winner = matchResults.get(matchKey);

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
        matchResults.put(matchKey, winner);
        losers.add(loser);

        // Check if all matches in the current round are complete
        List<Player> currentRound = rounds.get(roundIndex);
        boolean allMatchesComplete = true;
        for (int i = 0; i < currentRound.size(); i += 2) {
            if (i + 1 < currentRound.size()) {
                String key = roundIndex + "_" + currentRound.get(i).getId() + "_" + currentRound.get(i + 1).getId();
                if (!matchResults.containsKey(key)) {
                    allMatchesComplete = false;
                    break;
                }
            }
        }

        if (allMatchesComplete) {
            List<Player> nextRound = new ArrayList<>();
            if (roundIndex + 1 < rounds.size()) {
                nextRound = rounds.get(roundIndex + 1);
            } else {
                rounds.add(nextRound);
            }
            // Add winners to the next round
            for (int i = 0; i < currentRound.size(); i += 2) {
                if (i + 1 < currentRound.size()) {
                    String key = roundIndex + "_" + currentRound.get(i).getId() + "_" + currentRound.get(i + 1).getId();
                    Player matchWinner = matchResults.get(key);
                    if (matchWinner != null) {
                        nextRound.add(matchWinner);
                    }
                }
            }
            Collections.shuffle(nextRound); // Optional: Shuffle winners for next round
        }

        renderBracket();
    }

    private void advanceLoser(Player winner, Player loser) {
        String matchKey = "losers_" + winner.getId() + "_" + loser.getId();
        matchResults.put(matchKey, winner);
        losers.remove(loser);
        losers.remove(winner);
        losers.add(winner);
        renderBracket();
    }
}