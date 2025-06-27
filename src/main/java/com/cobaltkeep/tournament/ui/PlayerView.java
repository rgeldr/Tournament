package com.cobaltkeep.tournament.ui;

import com.cobaltkeep.tournament.entity.Player;
import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.service.PlayerService;
import com.cobaltkeep.tournament.service.TournamentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@Route("players")
public class PlayerView extends VerticalLayout implements HasUrlParameter<Long> {

    private final PlayerService playerService;
    private final TournamentService tournamentService;
    private Long tournamentId;
    private Tournament tournament;
    private final Grid<Player> assignedGrid = new Grid<>(Player.class);
    private final Grid<Player> availableGrid = new Grid<>(Player.class);
    private final TextField firstName = new TextField("First Name");
    private final TextField lastName = new TextField("Last Name");
    private final Button saveButton = new Button("Save");
    private final Button backButton = new Button("Back to Tournaments");
    private final Button deleteButton = new Button("Delete");
    private final Button addButton = new Button("Add to Tournament");
    private final Button startTournamentButton = new Button("Start Tournament");
    private final Binder<Player> binder = new Binder<>(Player.class);

    @Autowired
    public PlayerView(PlayerService playerService, TournamentService tournamentService) {
        this.playerService = playerService;
        this.tournamentService = tournamentService;

        // Configure assigned grid
        assignedGrid.setColumns("id");
        assignedGrid.addColumn(Player::getFullName).setHeader("Assigned Players");
        assignedGrid.asSingleSelect().addValueChangeListener(event ->
                deleteButton.setEnabled(event.getValue() != null));

        // Configure available grid with multi-selection
        availableGrid.setColumns("id");
        availableGrid.addColumn(Player::getFullName).setHeader("Available Players");
        availableGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        availableGrid.addSelectionListener(event -> {
            if (tournament != null) {
                addButton.setEnabled(!event.getAllSelectedItems().isEmpty() && !tournament.isLocked());
            }
        });

        // Configure form
        binder.forField(firstName).asRequired("First name is required").bind(Player::getFirstName, Player::setFirstName);
        binder.forField(lastName).asRequired("Last name is required").bind(Player::getLastName, Player::setLastName);

        // Configure buttons
        saveButton.addClickListener(event -> savePlayer());
        backButton.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("")));
        deleteButton.addClickListener(event -> deletePlayer());
        deleteButton.setEnabled(false);
        addButton.addClickListener(event -> addPlayer());
        addButton.setEnabled(false);
        startTournamentButton.addClickListener(event -> startTournament());

        // Layout: Include addButton below availableGrid
        add(assignedGrid, availableGrid, addButton,
                new HorizontalLayout(firstName, lastName, saveButton, deleteButton, backButton, startTournamentButton));
    }

    @Override
    public void setParameter(BeforeEvent event, Long tournamentId) {
        this.tournamentId = tournamentId;
        this.tournament = tournamentService.getTournamentById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        assignedGrid.setItems(tournament.getPlayers());
        availableGrid.setItems(playerService.getAvailablePlayers(tournamentId));
        Notification.show("Viewing players for tournament: " + tournament.getName());
        updateButtonStates();
    }

    private void savePlayer() {
        Player player = new Player();
        try {
            binder.writeBean(player);
            playerService.createPlayer(player, tournamentId);
            Notification.show("Player added to tournament");
            refreshGrids();
            updateButtonStates();
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
        Player selected = assignedGrid.asSingleSelect().getValue();
        if (selected != null) {
            try {
                tournament.getPlayers().remove(selected);
                selected.getTournaments().remove(tournament);
                tournamentService.createTournament(tournament);
                refreshGrids();
                Notification.show("Player removed from tournament");
                updateButtonStates();
                clearForm();
            } catch (Exception e) {
                Notification.show("Error removing player: " + e.getMessage());
            }
        }
    }

    private void addPlayer() {
        Set<Player> selectedPlayers = availableGrid.getSelectedItems();
        if (!selectedPlayers.isEmpty()) {
            try {
                for (Player player : selectedPlayers) {
                    playerService.addPlayerToTournament(player.getId(), tournamentId);
                }
                Notification.show("Players added to tournament");
                refreshGrids();
                updateButtonStates();
            } catch (Exception e) {
                Notification.show("Error adding players: " + e.getMessage());
            }
        }
    }

    private void startTournament() {
        try {
            tournament.setLocked(true);
            tournamentService.createTournament(tournament);
            Notification.show("Tournament started!");
            getUI().ifPresent(ui -> ui.navigate("bracket/" + tournamentId));
        } catch (Exception e) {
            Notification.show("Error starting tournament: " + e.getMessage());
        }
    }

    private void updateButtonStates() {
        boolean isLocked = tournament.isLocked();
        saveButton.setEnabled(!isLocked);
        deleteButton.setEnabled(!isLocked && assignedGrid.asSingleSelect().getValue() != null);
        addButton.setEnabled(!isLocked && !availableGrid.getSelectedItems().isEmpty());
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

    private void refreshGrids() {
        assignedGrid.setItems(tournament.getPlayers());
        availableGrid.setItems(playerService.getAvailablePlayers(tournamentId));
    }
}