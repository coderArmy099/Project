// MainController.java
package com.example.project;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import javafx.beans.property.*;
import javafx.scene.control.cell.CheckBoxListCell;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;


import javafx.scene.paint.Color;          // (new)
import javafx.scene.text.Font;

import javafx.scene.effect.DropShadow;
import javafx.scene.text.TextAlignment;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class CalendarController {

    @FXML private Button prevWeekBtn;
    @FXML private Button nextWeekBtn;
    @FXML private HBox   dayButtonsBox;


    private LocalDate weekStart;
    private final ToggleGroup dayToggleGroup = new ToggleGroup();
    private final List<ToggleButton> dateButtons = new ArrayList<>(7);
    private static final ZoneId APP_ZONE = ZoneId.of("GMT+6");
    @FXML private Label monthLabel;
    @FXML private Label yearLabel;

    @FXML private Button addTaskBtn;
    @FXML private VBox   addTaskPane;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;
    @FXML private TextField titleField;
    @FXML private Spinner<Integer>  hourSpin;
    @FXML private Spinner<Integer>  minSpin;
    @FXML private Spinner<Integer>  durSpin;
    @FXML private ChoiceBox<String> prioChoice;
    @FXML private ListView<Task> taskListView;
    @FXML private BorderPane rootPane;
    @FXML private DatePicker datePick;

    private LocalDate selectedDate;

    private static final Path TASK_FILE =
            Paths.get("Project/data/tasks.txt");

    private static final PseudoClass DONE = PseudoClass.getPseudoClass("completed");

    public static class Task {
        private final StringProperty  title = new SimpleStringProperty();
        private final BooleanProperty completed  = new SimpleBooleanProperty(false);
        private LocalDate date;
        private LocalTime start;
        private int duration;          // minutes
        private String priority;
        private String username;

        Task(String title, LocalDate date, LocalTime start,
             int duration, String priority, boolean completed, String username) {
            this.title.set(title);
            this.date = date;
            this.start = start;
            this.duration = duration;
            this.priority = priority;
            this.completed.set(completed);
            this.username = username;
        }
        /* getters … */
        public StringProperty titleProperty()     { return title; }
        public BooleanProperty completedProperty(){ return completed; }

        /* simple CSV (tab) representation */
        String toLine(){
            return String.join("\t",
                    username,
                    date.toString(),
                    completed.get()? "1":"0",
                    start.toString(),
                    Integer.toString(duration),
                    priority,
                    title.get().replace('\t',' '));
        }

        static Task fromLine(String line){
            String[] p = line.split("\t", 7);
            // 0=user 1=date 2=done 3=start 4=dura 5=prio 6=title
            return new Task(
                    p[6],
                    LocalDate.parse(p[1]),
                    LocalTime.parse(p[3]),
                    Integer.parseInt(p[4]),
                    p[5],
                    p[2].equals("1"),
                    p[0]
            );
        }
    }


    @FXML
    private void initialize() throws IOException {

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


        addTaskBtn.setOnAction(e -> toggleAddTaskForm());
        cancelBtn.setOnAction(e -> hideAddTaskForm());
        saveBtn.setOnAction(e -> {
            saveTaskFromForm();
            hideAddTaskForm();
        });


        Files.createDirectories(TASK_FILE.getParent());
        if (!Files.exists(TASK_FILE)) Files.createFile(TASK_FILE);

        taskListView.setCellFactory(list -> new TaskCell());


        datePick.setValue(LocalDate.now());



        /* when a task is toggled -> rewrite file */
        taskListView.setOnMouseClicked(e -> saveAllTasks());

        /* populate spinners & choice box once */
        hourSpin.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0,23,9));
        minSpin.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0,5));
        durSpin.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5,480,30,5));
        prioChoice.getItems().setAll("Low","Normal","High");
        prioChoice.setValue("Normal");

        addTaskPane.prefHeightProperty().bind(taskListView.heightProperty().multiply(0.8));
        addTaskPane.maxHeightProperty().bind(taskListView.heightProperty().multiply(0.6));
        addTaskPane.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.5));
        addTaskPane.maxWidthProperty().bind(rootPane.widthProperty().multiply(0.5));
        showTasksForDay(LocalDate.now(APP_ZONE));

        Label emptyLbl = new Label("No Task Scheduled\nClick the + icon to add a task");
        emptyLbl.setTextAlignment(TextAlignment.CENTER);
        emptyLbl.setAlignment(Pos.CENTER);
        emptyLbl.setWrapText(true);
        emptyLbl.setStyle("""
            -fx-text-fill: #b0b0b0;
            -fx-font-size: 34px;
            -fx-font-weight: 900;
            -fx-font-family: 'Artifakt Element Heavy';
            """);

        /* subtle cyan glow */
        emptyLbl.setEffect(new DropShadow(5, Color.TEAL));

        taskListView.setPlaceholder(emptyLbl);

        /* -------- drop shadow for the ListView's own borders -------- */
        taskListView.setEffect(new DropShadow(15, Color.color(0,0,0,0.75)));


    }

//    private final class TaskCell extends ListCell<Task> {
//
//        private final CheckBox check = new CheckBox();
//        private final Label timeLbl = new Label();
//        private final Label titleLbl = new Label();
//        private final Label prioLbl = new Label();
//        private final Label durLbl  = new Label();
//        private final VBox  texts   = new VBox(2, titleLbl,
//                new HBox(5, prioLbl, durLbl));
//        private final HBox  root    = new HBox(10, timeLbl, check, texts);
//
//        TaskCell() {
//            root.setAlignment(Pos.CENTER_LEFT);
//
//            /* teal start-time */
//            timeLbl.setTextFill(Color.TEAL);
//            timeLbl.setStyle("-fx-font-weight:bold;");
//
//            /* Lilita One for title (falls back to default if not installed) */
//            titleLbl.setFont(Font.font("Lilita One", 16));
//
//            /* small bottom line */
//            prioLbl.setStyle("-fx-font-size:12;");
//            durLbl.setStyle("-fx-font-size:12; -fx-font-style:italic;");
//
//            /* checkbox action → remove + save */
//            check.selectedProperty().addListener((obs, wasChecked, nowChecked) -> {
//                Task t = getItem();
//                if (t == null) return;
//                t.completed.set(nowChecked);     // model flag
//                pseudoClassStateChanged(DONE, nowChecked);   // CSS
//
//                saveAllTasks();
//            });
//
//        }
//
//        private void deleteTaskFromFile(Task task) {
//            try {
//                List<String> kept = Files.lines(TASK_FILE, StandardCharsets.UTF_8)
//                        .filter(line -> {
//                            /* split once – layout:  user<TAB>title<TAB>date<TAB>start … */
//                            String[] p = line.split("\t");
//                            if (p.length < 4) return true;
//                            return !(p[1].equals(task.title.get())
//                                    && p[2].equals(task.date.toString())
//                                    && p[3].equals(task.start.toString()));
//                        })
//                        .toList();
//
//                Files.write(TASK_FILE, kept,
//                        StandardCharsets.UTF_8,
//                        StandardOpenOption.TRUNCATE_EXISTING,
//                        StandardOpenOption.WRITE);
//            } catch (IOException ex) { ex.printStackTrace(); }
//        }
//
//
//
//        @Override
//        protected void updateItem(Task t, boolean empty) {
//            super.updateItem(t, empty);
//            if (empty || t == null) {
//                setGraphic(null);
//                return;
//            } else {
//                /* bind visual parts to task */
//                check.setSelected(t.completed.get());
//                timeLbl.setText(String.format("%02d:%02d",
//                        t.start.getHour(), t.start.getMinute()));
//
//                titleLbl.setText(t.title.get());
//
//                prioLbl.setText(t.priority + "  ");
//                switch (t.priority) {
//                    case "Low"    -> prioLbl.getStyleClass().setAll("priority-low");
//                    case "Normal" -> prioLbl.getStyleClass().setAll("priority-normal");
//                    case "High"   -> prioLbl.getStyleClass().setAll("priority-high");
//                }
//                durLbl.setText(t.duration + " min");
//
//                pseudoClassStateChanged(DONE, t.completed.get());   // <- NEW
//                setGraphic(root);
//            }
//        }
//    }

    private final class TaskCell extends ListCell<Task> {

        private final CheckBox check = new CheckBox();
        private final Label timeLbl = new Label();
        private final Label titleLbl = new Label();
        private final Label prioLbl = new Label();
        private final Label durLbl  = new Label();
        private final VBox  texts   = new VBox(2, titleLbl,
                new HBox(5, prioLbl, durLbl));
        private final HBox  root    = new HBox(10, timeLbl, check, texts);

        TaskCell() {
            // Apply main container styling
            root.getStyleClass().add("task-cell-container");

            // Apply individual component styles
            check.getStyleClass().add("task-checkbox");
            timeLbl.getStyleClass().add("task-time-label");
            titleLbl.getStyleClass().add("task-title-label");
            texts.getStyleClass().add("task-details-box");

            HBox infoBox = (HBox) texts.getChildren().get(1);
            infoBox.getStyleClass().add("task-info-box");

            prioLbl.getStyleClass().add("task-priority-label");
            durLbl.getStyleClass().add("task-duration-label");

            /* checkbox action → update completed state + styling */
            check.selectedProperty().addListener((obs, wasChecked, nowChecked) -> {
                Task t = getItem();
                if (t == null) return;
                t.completed.set(nowChecked);

                // Update visual state
                if (nowChecked) {
                    root.getStyleClass().add("completed");
                } else {
                    root.getStyleClass().remove("completed");
                }

                saveAllTasks();
            });
        }

        @Override
        protected void updateItem(Task t, boolean empty) {
            super.updateItem(t, empty);
            if (empty || t == null) {
                setGraphic(null);
                return;
            } else {
                /* bind visual parts to task */
                check.setSelected(t.completed.get());

                // Update completed styling
                if (t.completed.get()) {
                    root.getStyleClass().add("completed");
                } else {
                    root.getStyleClass().remove("completed");
                }

                timeLbl.setText(String.format("%02d:%02d",
                        t.start.getHour(), t.start.getMinute()));

                titleLbl.setText(t.title.get());

                prioLbl.setText(t.priority + "  ");
                // Clear previous priority styles
                prioLbl.getStyleClass().removeAll("priority-low", "priority-normal", "priority-high");
                switch (t.priority) {
                    case "Low"    -> prioLbl.getStyleClass().add("priority-low");
                    case "Normal" -> prioLbl.getStyleClass().add("priority-normal");
                    case "High"   -> prioLbl.getStyleClass().add("priority-high");
                }
                durLbl.setText(t.duration + " min");

                setGraphic(root);
            }
        }
    }


    private void saveTaskFromForm() {

        /* collect data from the form */
        String  title  = titleField.getText().trim();
        LocalDate date = datePick.getValue();
        if (title.isBlank() || date == null) return;               // nothing to save

        LocalTime start = LocalTime.of(hourSpin.getValue(), minSpin.getValue());
        int       dur   = durSpin.getValue();
        String    prio  = prioChoice.getValue();

        Task t = new Task(title, date, start, dur, prio, false, CurrentUser.username);

        /* 1 ── append to file */
        try (BufferedWriter w = Files.newBufferedWriter(TASK_FILE,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            w.write(t.toLine());
            w.newLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    /* 2 ── update ListView only when the task belongs to the day
           that is currently displayed                            */
        if (date.equals(selectedDate)) {
            taskListView.getItems().add(t);
        }

        hideAddTaskForm();     // close the sliding card
    }

    private void showTasksForDay(LocalDate date) {// called by your logic
        selectedDate = date; // store for later use
        fillTasksForDay(date);
    }

    private void fillTasksForDay(LocalDate date){
        String user = CurrentUser.username;
        taskListView.getItems().clear();

        try (BufferedReader r = Files.newBufferedReader(TASK_FILE)){
            r.lines()
                    .filter(s -> !s.isBlank())
                    .filter(s -> s.startsWith(user + "\t"))
                    .map(Task::fromLine)
                    .filter(t -> t.date.equals(date))
                    .sorted(Comparator.comparing(t -> t.start))
                    .forEach(taskListView.getItems()::add);
        } catch (IOException ex){ ex.printStackTrace(); }
    }


    private void saveAllTasks() {
        Map<String,Task> map = new LinkedHashMap<>();
        try (BufferedReader r = Files.newBufferedReader(TASK_FILE)) {
            r.lines().filter(s -> !s.isBlank())
                    .map(Task::fromLine)
                    .forEach(t -> map.put(signatureOf(t), t));
        } catch (IOException ignored) { }

        taskListView.getItems()
                .forEach(t -> map.put(signatureOf(t), t));

        /* 3 ── finally rewrite the whole file without any duplicates        */
        try (BufferedWriter w = Files.newBufferedWriter(TASK_FILE)) {
            for (Task t : map.values()) {
                w.write(t.toLine());
                w.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private static String signatureOf(Task t) {
        return String.join("|",
                t.date.toString(),
                t.start.toString(),
                String.valueOf(t.duration),
                t.priority,
                t.title.get()
        ).toLowerCase();
    }




    private void updateHeader(LocalDate date) {
        monthLabel.setText(
                date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        );
        yearLabel.setText(String.valueOf(date.getYear()));
    }





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


    private void fillButtons() {
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            dateButtons.get(i).setText(String.valueOf(date.getDayOfMonth()));
        }
    }

    private void moveWeek(int delta) {
        int selectedOffset = 0;
        Toggle sel = dayToggleGroup.getSelectedToggle();
        if (sel != null) selectedOffset = (int) sel.getUserData();

        weekStart = weekStart.plusWeeks(delta);
        fillButtons();

        dateButtons.get(selectedOffset).setSelected(true);
        LocalDate newDate = weekStart.plusDays(selectedOffset);

        updateHeader(newDate);
        showTasksForDay(newDate);
    }


    private void toggleAddTaskForm() {
        boolean show = !addTaskPane.isVisible();
        addTaskPane.setVisible(show);
        addTaskPane.setManaged(show);
        if (show) {
            titleField.requestFocus();
        }
    }

    private void hideAddTaskForm() {
        addTaskPane.setVisible(false);
        addTaskPane.setManaged(false);
    }

    @FXML
    private void goToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToCourseTracker(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CourseTracker.fxml"));
            Parent root = loader.load();
            CourseTrackerController controller = loader.getController();
            if (CurrentUser.username != null && !CurrentUser.username.isEmpty()) {
                controller.setUsername(CurrentUser.username);
            } else {
                controller.setUsername("defaultUser");
            }
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToStudyTimer(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("studyTimer.fxml"));
            Parent root = loader.load();
            StudyTimerController controller = loader.getController();
            if (CurrentUser.username != null && !CurrentUser.username.isEmpty()) {
                controller.setUsername(CurrentUser.username);
            } else {
                controller.setUsername("defaultUser");
            }
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToCommunity(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Rooms.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}