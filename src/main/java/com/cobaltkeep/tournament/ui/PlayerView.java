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
    private final TextField name = new TextField("Player Name");
    private final TextField email = new TextField("Player Email");
    private final Button saveButton = new Button("Save");
    private final Button backButton = new Button("Back to Tournaments");
    private final Binder<Player> binder = new Binder<>(Player.class);
    private final Button deleteButton = new Button("Delete");

    @Autowired
    public PlayerView(PlayerService playerService, TournamentService tournamentService) {
        this.playerService = playerService;
        this.tournamentService = tournamentService;

        // Configure grid
        grid.setColumns("id", "name", "email");

        // Configure form
        binder.forField(name).asRequired("Name is required").bind(Player::getName, Player::setName);
        binder.forField(email)
                .withValidator(email -> email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"), "Invalid email format")
                .bind(Player::getEmail, Player::setEmail);

        // Configure buttons
        saveButton.addClickListener(event -> savePlayer());
        backButton.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("")));

        // Layout
        HorizontalLayout formLayout = new HorizontalLayout(name, email, saveButton, backButton);
        add(grid, formLayout);

        deleteButton.addClickListener(event -> deletePlayer());
        deleteButton.setEnabled(false);
        grid.asSingleSelect().addValueChangeListener(event -> deleteButton.setEnabled(event.getValue() != null));
        formLayout.add(deleteButton);
    }

    @Override
    public void setParameter(BeforeEvent event, Long tournamentId) {
        this.tournamentId = tournamentId;
        // Load players for this tournament
        Tournament tournament = tournamentService.getTournamentById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        grid.setItems(tournament.getPlayers());
        Notification.show("Viewing players for tournament: " + tournament.getName());

        // Check if there are no players and prompt to add one
        if (tournament.getPlayers().isEmpty()) {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("No Players Found");
            dialog.setWidth("400px");
            VerticalLayout dialogLayout = new VerticalLayout();
            dialogLayout.add("This tournament has no players. Would you like to add a new player?");
            dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            Button addPlayerButton = new Button("Add Player", e -> {
                name.focus();
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
            // Refresh grid
            Tournament tournament = tournamentService.getTournamentById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
            grid.setItems(tournament.getPlayers());
            clearForm();
        } catch (ValidationException e) {
            Notification.show("Validation error: " + e.getMessage());
        } catch (Exception e) {
            Notification.show("Error saving player: " + e.getMessage());
        }
    }

    private void clearForm() {
        binder.removeBean();
        name.clear();
        email.clear();
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
                clearForm();
            } catch (Exception e) {
                Notification.show("Error removing player: " + e.getMessage());
            }
        }
    }

}