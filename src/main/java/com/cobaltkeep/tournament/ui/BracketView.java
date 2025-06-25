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
import java.util.List;
import java.util.Set;

@Route("bracket")
public class BracketView extends VerticalLayout implements HasUrlParameter<Long> {

    private final TournamentService tournamentService;
    private Long tournamentId;
    private List<List<Player>> rounds = new ArrayList<>();
    private List<Player> losers = new ArrayList<>();

    @Autowired
    public BracketView(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @Override
    public void setParameter(BeforeEvent event, Long tournamentId) {
        this.tournamentId = tournamentId;
        Tournament tournament = tournamentService.getTournamentById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        initializeBracket(new ArrayList<>(tournament.getPlayers())); // Convert Set to List
        renderBracket();
    }

    private void initializeBracket(List<Player> players) {
        rounds.clear();
        losers.clear();
        List<Player> randomizedPlayers = new ArrayList<>(players);
        Collections.shuffle(randomizedPlayers);
        rounds.add(randomizedPlayers);
    }

    private void renderBracket() {
        removeAll();
        add(new H3("Tournament Bracket"));

        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            List<Player> round = rounds.get(roundIndex);
            VerticalLayout roundLayout = new VerticalLayout();
            roundLayout.add(new H3("Round " + (roundIndex + 1)));

            for (int i = 0; i < round.size(); i += 2) {
                if (i + 1 < round.size()) {
                    Player player1 = round.get(i);
                    Player player2 = round.get(i + 1);
                    HorizontalLayout matchLayout = new HorizontalLayout();
                    int finalRoundIndex = roundIndex;
                    Button winner1Button = new Button(player1.getName(), e -> advancePlayer(player1, player2, finalRoundIndex));
                    int finalRoundIndex1 = roundIndex;
                    Button winner2Button = new Button(player2.getName(), e -> advancePlayer(player2, player1, finalRoundIndex1));
                    matchLayout.add(winner1Button, new Button("vs"), winner2Button);
                    roundLayout.add(matchLayout);
                }
            }
            add(roundLayout);
        }

        // Losers bracket
        if (!losers.isEmpty()) {
            VerticalLayout losersLayout = new VerticalLayout();
            losersLayout.add(new H3("Losers Bracket"));
            List<Player> randomizedLosers = new ArrayList<>(losers);
            Collections.shuffle(randomizedLosers);
            for (int i = 0; i < randomizedLosers.size(); i += 2) {
                if (i + 1 < randomizedLosers.size()) {
                    Player player1 = randomizedLosers.get(i);
                    Player player2 = randomizedLosers.get(i + 1);
                    HorizontalLayout matchLayout = new HorizontalLayout();
                    Button winner1Button = new Button(player1.getName(), e -> advanceLoser(player1, player2));
                    Button winner2Button = new Button(player2.getName(), e -> advanceLoser(player2, player1));
                    matchLayout.add(winner1Button, new Button("vs"), winner2Button);
                    losersLayout.add(matchLayout);
                }
            }
            add(losersLayout);
        }
    }

    private void advancePlayer(Player winner, Player loser, int roundIndex) {
        losers.add(loser);
        List<Player> currentRound = rounds.get(roundIndex);
        currentRound.remove(winner);
        currentRound.remove(loser);

        if (currentRound.isEmpty()) {
            List<Player> nextRound = new ArrayList<>();
            if (roundIndex + 1 < rounds.size()) {
                nextRound = rounds.get(roundIndex + 1);
            } else {
                rounds.add(nextRound);
            }
            nextRound.add(winner);
            if (nextRound.size() % 2 == 0 && !nextRound.isEmpty()) {
                renderBracket();
            }
        }

        if (rounds.get(roundIndex).isEmpty() && rounds.size() == roundIndex + 1 && rounds.get(roundIndex).size() == 1) {
            add(new H3("Winner: " + winner.getName()));
        }
        renderBracket();
    }

    private void advanceLoser(Player winner, Player loser) {
        losers.remove(loser);
        losers.remove(winner);
        losers.add(winner);
        renderBracket();
    }
}