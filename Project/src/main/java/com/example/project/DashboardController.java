package com.example.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Priority;
import java.time.ZoneId;

public class DashboardController {

    @FXML private BorderPane rootPane;
    @FXML private StackPane rootStack;
    @FXML private VBox navPane;
    @FXML private Button dashboardBtn;
    @FXML private Button courseTrackerNavBtn;
    @FXML private Button calendarBtn;
    @FXML private Button timerBtn;
    @FXML private Button communityBtn;
    @FXML private HBox userInfoBox;
    @FXML private Label usernameLabel;
    @FXML private Label schoolLabel;
    @FXML private Button logoutBtn;
    @FXML private Label monthlyStudyTimeLabel;
    @FXML private VBox todayTasksContainer;
    @FXML private VBox courseProgressContainer;

    private final String BASE_DATA_PATH = "data/";


    public void initialize() {
        if (dashboardBtn != null) dashboardBtn.setOnAction(this::goToDashboard);
        if (courseTrackerNavBtn != null) courseTrackerNavBtn.setOnAction(this::goToCourseTracker);
        if (calendarBtn != null) calendarBtn.setOnAction(this::goToCalendar);
        if (timerBtn != null) timerBtn.setOnAction(this::goToStudyTimer);
        if (communityBtn != null) communityBtn.setOnAction(this::goToCommunity);
        // Setup logout button and user info
        if (logoutBtn != null) logoutBtn.setOnAction(this::handleLogout);
        loadUserInfo();
        loadDashboardData();
    }


    private void loadDashboardData() {
        loadMonthlyStudyTime();
        loadTodaysTasks();
        loadCourseProgress();
    }

    private void loadMonthlyStudyTime() {
        if (CurrentUser.getUsername() == null) return;

        String userRecordsFile = BASE_DATA_PATH + CurrentUser.getUsername() + "_study_records.txt";
        Path recordFilePath = Paths.get(userRecordsFile);

        int totalSeconds = 0;
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        try {
            if (Files.exists(recordFilePath) && Files.size(recordFilePath) > 0) {
                try (BufferedReader reader = Files.newBufferedReader(recordFilePath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length == 5) {
                            String timestamp = parts[1];
                            String time = parts[2];

                            if (timestamp.startsWith(currentMonth)) {
                                totalSeconds += parseTimeToSeconds(time);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading study records: " + e.getMessage());
        }

        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        monthlyStudyTimeLabel.setText(String.format("Monthly Study Time: %d:%02d:%02d", hours, minutes, seconds));
    }

    private void loadTodaysTasks() {
        if (CurrentUser.getUsername() == null) return;

        todayTasksContainer.getChildren().clear();
        String tasksFile = "Project/data/tasks.txt";
        Path tasksFilePath = Paths.get(tasksFile);

        String today = LocalDate.now(java.time.ZoneId.of("GMT+6")).toString();

        try {
            if (Files.exists(tasksFilePath)) {
                List<String> lines = Files.readAllLines(tasksFilePath, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split("\t");
                    if (parts.length >= 7) {
                        String taskUsername = parts[0];
                        String taskDate = parts[1];
                        String completed = parts[2];
                        String taskDescription = parts[6];
                        String priority = parts[5];
                        String starttime = parts[3];

                        if (taskUsername.equals(CurrentUser.getUsername()) &&
                                taskDate.equals(today) &&
                                completed.equals("0")) {  // Only uncompleted tasks

                            HBox taskItem = createTaskItem(taskDescription, priority, starttime,false);
                            todayTasksContainer.getChildren().add(taskItem);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading tasks: " + e.getMessage());
        }

        if (todayTasksContainer.getChildren().isEmpty()) {
            Label noTasksLabel = new Label("No pending tasks for today");
            noTasksLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14px;");
            todayTasksContainer.getChildren().add(noTasksLabel);
        }
    }





    private void loadCourseProgress() {
        if (CurrentUser.getUsername() == null) return;

        courseProgressContainer.getChildren().clear();
        String coursesFile = BASE_DATA_PATH + "courses_" + CurrentUser.getUsername() + ".txt";
        Path coursesFilePath = Paths.get(coursesFile);

        try {
            if (Files.exists(coursesFilePath)) {
                try (BufferedReader reader = Files.newBufferedReader(coursesFilePath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Course course = Course.fromCsvString(line);
                        if (course != null) {
                            HBox courseItem = createCourseProgressItem(course);
                            courseProgressContainer.getChildren().add(courseItem);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading courses: " + e.getMessage());
        }

        if (courseProgressContainer.getChildren().isEmpty()) {
            Label noCoursesLabel = new Label("No courses added yet");
            noCoursesLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14px;");
            courseProgressContainer.getChildren().add(noCoursesLabel);
        }
    }

    private HBox createTaskItem(String taskDescription,String priority, String starttime, boolean isCompleted) {
        HBox taskItem = new HBox(8);
        taskItem.setAlignment(Pos.CENTER_LEFT);
        taskItem.getStyleClass().add("task-item");
        taskItem.setStyle("-fx-padding: 8;");
        // Remove checkbox, just show task as text
        Label taskLabel = new Label(taskDescription);
        taskLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-font-family: 'Artifakt Element'");

        Label priorityLabel = new Label(priority);
        priorityLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-font-family: 'Artifakt Element'");

        Label startTimeLabel = new Label(starttime);
        startTimeLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-font-family: 'Artifakt Element'");

        Label dot = new Label("•");
        dot.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 20px; -fx-font-family: 'Artifakt Element'");

        Label dot2 = new Label("•");
        dot2.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 20px; -fx-font-family: 'Artifakt Element'");


        taskItem.getChildren().add(startTimeLabel);
        taskItem.getChildren().add(dot);
        taskItem.getChildren().add(taskLabel);
        taskItem.getChildren().add(dot2);
        taskItem.getChildren().add(priorityLabel);
        return taskItem;
    }

    private HBox createCourseProgressItem(Course course) {
        HBox courseItem = new HBox(15);
        courseItem.setAlignment(Pos.CENTER_LEFT);
        courseItem.getStyleClass().add("task-item");
        courseItem.setStyle("-fx-padding: 12;");

        VBox courseInfo = new VBox(5);
        Label courseNameLabel = new Label(course.getCourseName());
        courseNameLabel.setStyle("-fx-text-fill: #00bfa6; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Artifakt Element'");

        Label progressLabel = new Label(String.format("Progress: %.0f%%", course.getProgress() * 100));
        progressLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-font-family: 'Artifakt Element'");

        courseInfo.getChildren().addAll(courseNameLabel, progressLabel);

        ProgressBar progressBar = new ProgressBar(course.getProgress());
        progressBar.setPrefWidth(500);
//        progressBar.getStyleClass().add("course-progress-bar");
        progressBar.getStyleClass().add("course-progress-bar");

        courseItem.getChildren().addAll(courseInfo, progressBar);
        HBox.setHgrow(courseInfo, Priority.ALWAYS);

        return courseItem;
    }

    private int parseTimeToSeconds(String timeString) {
        String[] parts = timeString.split(":");
        if (parts.length == 3) {
            try {
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int s = Integer.parseInt(parts[2]);
                return h * 3600 + m * 60 + s;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing time string: " + timeString);
                return 0;
            }
        }
        return 0;
    }

    private void loadUserInfo() {
        if (usernameLabel != null && CurrentUser.getUsername() != null) {
            usernameLabel.setText(CurrentUser.getUsername());
        }
        if (schoolLabel != null && CurrentUser.getSchool() != null) {
            schoolLabel.setText(CurrentUser.getSchool());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Clear current user data
        CurrentUser.clearUserData();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("start.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Logout Error", "Could not return to login page.");
        }
    }

    @FXML
    private void goToDashboard(ActionEvent event) {
        // Already on Dashboard, no action needed
    }

    @FXML
    private void goToCourseTracker(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CourseTracker.fxml"));
            Parent root = loader.load();
            CourseTrackerController controller = loader.getController();
            if (CurrentUser.getUsername() != null && !CurrentUser.getUsername().isEmpty()) {
                controller.setUsername(CurrentUser.getUsername());
            } else {
                controller.setUsername("defaultUser");
            }
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load Course Tracker page.");
        }
    }

    @FXML
    private void goToCalendar(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Calendar.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load Calendar page.");
        }
    }

    @FXML
    private void goToStudyTimer(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("studyTimer.fxml"));
            Parent root = loader.load();
            StudyTimerController controller = loader.getController();
            if (CurrentUser.getUsername() != null && !CurrentUser.getUsername().isEmpty()) {
                controller.setUsername(CurrentUser.getUsername());
            } else {
                controller.setUsername("defaultUser");
            }
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load Study Timer page.");
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
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load Community page.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
