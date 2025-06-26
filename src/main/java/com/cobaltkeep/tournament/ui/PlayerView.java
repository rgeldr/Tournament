package com.cobaltkeep.tournament.ui;

import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.service.PlayerService;
import com.cobaltkeep.tournament.service.TournamentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route("players")
public class PlayerView extends VerticalLayout implements HasUrlParameter<Long> {

    private final PlayerService playerService;
    private final TournamentService tournamentService;
    private Long tournamentId;
    private final Grid<Player> grid = new Grid<>(Player.class);
    private final TextField firstName = new TextField("First Name");
    private final TextField lastName = new TextField("Last Name");
    private final Button saveButton = new Button("Save");
    private final Button backButton = new Button("Back to Tournaments");
    private final Button deleteButton = new Button("Delete");
    private final Button startTournamentButton = new Button("Start Tournament");
    private final Binder<Player> binder = new Binder<>(Player.class);

    @Autowired
    public PlayerView(PlayerService playerService, TournamentService tournamentService) {
        this.playerService = playerService;
        this.tournamentService = tournamentService;

        // Configure grid
        grid.setColumns("id");
        grid.addColumn(Player::getFullName).setHeader("Name");

        // Configure form
        binder.forField(firstName).asRequired("First name is required").bind(Player::getFirstName, Player::setFirstName);
        binder.forField(lastName).asRequired("Last name is required").bind(Player::getLastName, Player::setLastName);

        // Configure buttons
        saveButton.addClickListener(event -> savePlayer());
        backButton.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("")));
        deleteButton.addClickListener(event -> deletePlayer());
        deleteButton.setEnabled(false);
        grid.asSingleSelect().addValueChangeListener(event -> deleteButton.setEnabled(event.getValue() != null));
        startTournamentButton.addClickListener(event -> startTournament());

        // Layout
        HorizontalLayout formLayout = new HorizontalLayout(firstName, lastName, saveButton, deleteButton, backButton, startTournamentButton);
        add(grid, formLayout);
    }

    @Override
    public void setParameter(BeforeEvent event, Long tournamentId) {
        this.tournamentId = tournamentId;
        // Load players for this tournament
        Tournament tournament = tournamentService.getTournamentById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        grid.setItems(tournament.getPlayers());
        Notification.show("Viewing players for tournament: " + tournament.getName());

        // Update button states
        updateButtonStates(tournament);

        // Check if there are no players and prompt to add one
        if (tournament.getPlayers().isEmpty()) {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("No Players Found");
            dialog.setWidth("400px");
            VerticalLayout dialogLayout = new VerticalLayout();
            dialogLayout.add("This tournament has no players. Would you like to add a new player?");
            dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            Button addPlayerButton = new Button("Add Player", e -> {
                firstName.focus();
                dialog.close();
            });
            Button cancelButton = new Button("Cancel", e -> dialog.close());
            dialogLayout.add(new HorizontalLayout(addPlayerButton, cancelButton));
            dialog.add(dialogLayout);
            dialog.open();
        }
    }

    private void savePlayer() {
        Player player = new Player();
        try {
            binder.writeBean(player);
            playerService.createPlayer(player, tournamentId);
            Notification.show("Player added to tournament");
            // Refresh grid and button states
            Tournament tournament = tournamentService.getTournamentById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
            grid.setItems(tournament.getPlayers());
            updateButtonStates(tournament);
            clearForm();
        } catch (ValidationException e) {
            Notification.show("Validation error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            Notification.show(e.getMessage());
        } catch (Exception e) {
            Notification.show("Error saving player: " + e.getMessage());
        }
    }

    private void deletePlayer() {
        Player selected = grid.asSingleSelect().getValue();
        if (selected != null) {
            try {
                Tournament tournament = tournamentService.getTournamentById(tournamentId)
                        .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
                tournament.getPlayers().remove(selected);
                selected.getTournaments().remove(tournament);
                tournamentService.createTournament(tournament);
                grid.setItems(tournament.getPlayers());
                Notification.show("Player removed from tournament");
                updateButtonStates(tournament);
                clearForm();
            } catch (Exception e) {
                Notification.show("Error removing player: " + e.getMessage());
            }
        }
    }

    private void startTournament() {
        try {
            Tournament tournament = tournamentService.getTournamentById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
            tournament.setLocked(true);
            tournamentService.createTournament(tournament);
            Notification.show("Tournament started!");
            getUI().ifPresent(ui -> ui.navigate("bracket/" + tournamentId));
        } catch (Exception e) {
            Notification.show("Error starting tournament: " + e.getMessage());
        }
    }

    private void updateButtonStates(Tournament tournament) {
        saveButton.setEnabled(!tournament.isLocked());
        deleteButton.setEnabled(!tournament.isLocked() && grid.asSingleSelect().getValue() != null);
        int playerCount = tournament.getPlayers().size();
        boolean canStart = playerCount >= 4 && playerCount % 2 == 0;
        startTournamentButton.setEnabled(canStart);
        startTournamentButton.setTooltipText(canStart ? "Start the tournament" :
                playerCount < 4 ? "Need at least 4 players" : "Need an even number of players");
    }

    private void clearForm() {
        binder.removeBean();
        firstName.clear();
        lastName.clear();
    }
}