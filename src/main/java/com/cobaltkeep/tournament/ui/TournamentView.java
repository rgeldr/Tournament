package com.cobaltkeep.tournament.ui;

import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.service.TournamentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToDateConverter;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Route("")
public class TournamentView extends VerticalLayout {

    private final TournamentService tournamentService;
    private final Grid<Tournament> grid = new Grid<>(Tournament.class);
    private final TextField name = new TextField("Tournament Name");
    private final TextField startDate = new TextField("Start Date (YYYY-MM-DD)");
    private final TextField endDate = new TextField("End Date (YYYY-MM-DD)");
    private final Button saveButton = new Button("Save");
    private final Button clearButton = new Button("Clear");
    private final Button deleteButton = new Button("Delete");
    private final Binder<Tournament> binder = new Binder<>(Tournament.class);

    @Autowired
    public TournamentView(TournamentService tournamentService) {
        this.tournamentService = tournamentService;

        // Configure grid
        // Clear all default columns to avoid duplicates
        grid.removeAllColumns();
        // Add custom name column with RouterLink
        grid.addColumn(new ComponentRenderer<>(tournament -> {
            RouterLink link = new RouterLink(tournament.getName(), PlayerView.class, tournament.getId());
            return link;
        })).setHeader("Name");
        // Add other columns explicitly
        grid.addColumn(Tournament::getId).setHeader("ID");
        grid.addColumn(Tournament::getStartDate).setHeader("Start Date");
        grid.addColumn(Tournament::getEndDate).setHeader("End Date");
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                binder.readBean(event.getValue());
                deleteButton.setEnabled(true);
            } else {
                clearForm();
            }
        });

        // Configure form
        binder.forField(name).asRequired("Name is required").bind(Tournament::getName, Tournament::setName);
        binder.forField(startDate)
                .withConverter(new StringToDateConverter(DateTimeFormatter.ISO_LOCAL_DATE, "Invalid date format, use YYYY-MM-DD"))
                .bind(Tournament::getStartDate, Tournament::setStartDate);
        binder.forField(endDate)
                .withConverter(new StringToDateConverter(DateTimeFormatter.ISO_LOCAL_DATE, "Invalid date format, use YYYY-MM-DD"))
                .bind(Tournament::getEndDate, Tournament::setEndDate);

        // Configure buttons
        saveButton.addClickListener(event -> saveTournament());
        clearButton.addClickListener(event -> clearForm());
        deleteButton.addClickListener(event -> deleteTournament());
        deleteButton.setEnabled(false);

        // Layout
        HorizontalLayout formLayout = new HorizontalLayout(name, startDate, endDate, saveButton, clearButton, deleteButton);
        add(grid, formLayout);

        // Load data
        updateGrid();
    }

    private void updateGrid() {
        grid.setItems(tournamentService.getAllTournaments());
    }

    private void saveTournament() {
        Tournament tournament = new Tournament();
        try {
            binder.writeBean(tournament);
            if (tournament.getId() == null) {
                tournamentService.createTournament(tournament);
                Notification.show("Tournament created");
            } else {
                tournamentService.updateTournament(tournament.getId(), tournament);
                Notification.show("Tournament updated");
            }
            updateGrid();
            clearForm();
        } catch (ValidationException e) {
            Notification.show("Validation error: " + e.getMessage());
        } catch (Exception e) {
            Notification.show("Error saving tournament: " + e.getMessage());
        }
    }

    private void deleteTournament() {
        Tournament selected = grid.asSingleSelect().getValue();
        if (selected != null) {
            try {
                tournamentService.deleteTournament(selected.getId());
                Notification.show("Tournament deleted");
                updateGrid();
                clearForm();
            } catch (Exception e) {
                Notification.show("Error deleting tournament: " + e.getMessage());
            }
        }
    }

    private void clearForm() {
        binder.removeBean();
        name.clear();
        startDate.clear();
        endDate.clear();
        deleteButton.setEnabled(false);
    }
}