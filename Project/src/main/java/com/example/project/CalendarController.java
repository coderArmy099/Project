// MainController.java
package com.example.project;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;


public class CalendarController {

    /* --------------- FXML refs --------------- */
    @FXML private Button prevWeekBtn;
    @FXML private Button nextWeekBtn;
    @FXML private HBox   dayButtonsBox;

    /* --------------- internal state --------------- */
    private LocalDate weekStart;                 // Sunday of the currently shown week
    private final ToggleGroup dayToggleGroup = new ToggleGroup();
    private final List<ToggleButton> dateButtons = new ArrayList<>(7);
    private static final ZoneId APP_ZONE = ZoneId.of("GMT+6");
    /* extra FXML refs for the header */
    @FXML private Label monthLabel;
    @FXML private Label yearLabel;


    @FXML
    private void initialize() {

        // today in the chosen zone
        LocalDate today = LocalDate.now(APP_ZONE);

        // Sunday of *this* week (previous or same)
        weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        createDayButtons();
        fillButtons();

        // select the current day
        int todayOffset = (int) ChronoUnit.DAYS.between(weekStart, today);
        dateButtons.get(todayOffset).setSelected(true);

        updateHeader(today);

        prevWeekBtn.setOnAction(e -> moveWeek(-1));
        nextWeekBtn.setOnAction(e -> moveWeek( 1));
    }

    /* ── HEADER (month + year) ───────────────────────────────────────── */
    private void updateHeader(LocalDate date) {
        monthLabel.setText(
                date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        );
        yearLabel.setText(String.valueOf(date.getYear()));
    }




    /* -------------- build weekday buttons only once -------------- */
    private void createDayButtons() {
        for (int i = 0; i < 7; i++) {

            // layout: VBox (button on top, label under)
            ToggleButton btn = new ToggleButton("##");
            btn.getStyleClass().add("date-button");
            btn.setToggleGroup(dayToggleGroup);
            btn.setUserData(i);          // store offset 0-6 for later

            /* CORRECT weekday label: start with Sunday, then +1 each time */
            DayOfWeek dow = DayOfWeek.SUNDAY.plus(i);    // 0→SUN, 1→MON, …
            Label lbl = new Label(
                    dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                            .toLowerCase()
            );
            lbl.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 11;");

            VBox cell = new VBox(4, btn, lbl);
            cell.setAlignment(javafx.geometry.Pos.CENTER);

            dayButtonsBox.getChildren().add(cell);
            dateButtons.add(btn);
        }

        /* default selection = today (use the same time-zone!) */
        LocalDate today = LocalDate.now(APP_ZONE);
        int todayOffset = (int) ChronoUnit.DAYS.between(weekStart, today);

        if (0 <= todayOffset && todayOffset < 7) {
            dateButtons.get(todayOffset).setSelected(true);
        } else {
            dateButtons.get(0).setSelected(true);        // fallback: Sunday
        }

        // react to day change
        dayToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) {
                showTasksForDay(weekStart.plusDays((int) newT.getUserData()));
            }
        });

    }

    /* -------------- update captions when week changes -------------- */
    private void fillButtons() {
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            dateButtons.get(i).setText(String.valueOf(date.getDayOfMonth()));
        }
    }

    /* ── WEEK NAVIGATION (with header refresh) ───────────────────────── */
    private void moveWeek(int delta) {
        int selectedOffset = 0;
        Toggle sel = dayToggleGroup.getSelectedToggle();
        if (sel != null) selectedOffset = (int) sel.getUserData();

        weekStart = weekStart.plusWeeks(delta);
        fillButtons();

        dateButtons.get(selectedOffset).setSelected(true);
        LocalDate newDate = weekStart.plusDays(selectedOffset);

        updateHeader(newDate);          // <─ keep labels in sync
        showTasksForDay(newDate);
    }


    /* -------------- placeholder for your own logic -------------- */
    private void showTasksForDay(LocalDate date) {
        // TODO: populate calendarGrid / task list for the given date
        System.out.println("Show tasks for " + date);
    }
}