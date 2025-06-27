package com.cobaltkeep.tournament.ui;

import com.cobaltkeep.tournament.entity.Tournament;
import com.cobaltkeep.tournament.service.TournamentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

@Route("")
public class TournamentView extends VerticalLayout {

    private final TournamentService tournamentService;
    private final Grid<Tournament> grid = new Grid<>(Tournament.class);
    private final TextField name = new TextField("Tournament Name");
    private final DatePicker startDate = new DatePicker("Start Date");
    private final DatePicker endDate = new DatePicker("End Date");
    private final Button saveButton = new Button("Save");
    private final Button clearButton = new Button("Clear");
    private final Button deleteButton = new Button("Delete");
    private final Binder<Tournament> binder = new Binder<>(Tournament.class);

    @Autowired
    public TournamentView(TournamentService tournamentService) {
        this.tournamentService = tournamentService;

        // Navigation tabs
        Tabs tabs = new Tabs();
        Tab tournamentsTab = new Tab(new RouterLink("Tournaments", TournamentView.class));
        Tab playersTab = new Tab(new RouterLink("Players", PlayerView.class, 0L)); // Placeholder ID
        tabs.add(tournamentsTab, playersTab);
        tabs.setWidthFull();
        add(tabs);
        setAlignItems(FlexComponent.Alignment.STRETCH);

        // Configure grid
        grid.setWidthFull();
        grid.removeAllColumns();
        grid.addColumn(new ComponentRenderer<>(tournament -> {
            RouterLink link = new RouterLink(tournament.getName(), PlayerView.class, tournament.getId());
            return link;
        })).setHeader("Name");
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
                .asRequired("Start date is required")
                .bind(Tournament::getStartDate, Tournament::setStartDate);
        binder.forField(endDate)
                .bind(Tournament::getEndDate, Tournament::setEndDate);

        // Configure buttons
        saveButton.addClickListener(event -> saveTournament());
        clearButton.addClickListener(event -> clearForm());
        deleteButton.addClickListener(event -> deleteTournament());
        deleteButton.setEnabled(false);

        // Layout
        HorizontalLayout formLayout = new HorizontalLayout(name, startDate, endDate, saveButton, clearButton, deleteButton);
        formLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        name.setWidth("200px");
        startDate.setWidth("150px");
        endDate.setWidth("150px");
        add(grid, formLayout);

        // Load data
        updateGrid();
    }

    private void updateGrid() {
        grid.setItems(tournamentService.getAllTournaments());
    }

    private void saveTournament() {
        try {
            if (binder.getBean() == null || binder.getBean().getId() == null) {
                // Creating new tournament
                Tournament tournament = new Tournament();
                binder.writeBean(tournament);
                tournamentService.createTournament(tournament);
                Notification.show("Tournament created");
            } else {
                // Updating existing tournament
                Tournament existingTournament = binder.getBean();
                Tournament updatedTournament = new Tournament();
                binder.writeBean(updatedTournament);
                // Preserve existing data
                updatedTournament.setId(existingTournament.getId());
                updatedTournament.setPlayers(existingTournament.getPlayers());
                updatedTournament.setLocked(existingTournament.isLocked());
                tournamentService.updateTournament(existingTournament.getId(), updatedTournament);
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
        startDate.setValue(null);
        endDate.setValue(null);
        deleteButton.setEnabled(false);
    }
}