package com.example.project;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import org.kordamp.ikonli.javafx.FontIcon; // Import FontIcon

public class StudyTimerController {
    private String username;
    @FXML private Label timerLabel;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button resetButton;
    @FXML private ComboBox<String> courseSelectionComboBox;
    @FXML private ComboBox<String> topicSelectionComboBox;
    @FXML private Label selectedCourseTopicLabel;
    @FXML private Label noCoursesMessageLabel;

    @FXML private Canvas timerCanvas;

    // FXML references for the FontIcons within the buttons
    @FXML private FontIcon startIcon; // For play/pause/resume icon


    @FXML private Button calendarNavBtn;
    @FXML private Button courseTrackerNavBtn;
    @FXML private Button timerNavBtn;
    @FXML private Button dashboardNavBtn;
    @FXML private Button communityBtn;


    private Timeline timerTimeline;
    private AnimationTimer animationTimer;
    private long animationStartTimeNanos;
    private final double ANIMATION_CYCLE_SECONDS = 2.0;

    private int hours = 0, minutes = 0, seconds = 0;
    private boolean running = false;
    private final String BASE_DATA_PATH = "data/";
    private String USER_RECORDS_FILE;
    private String USER_COURSES_FILE;

    private Map<String, List<String>> userCoursesAndTopics;
    private Map<String, Integer> bestStudyTimesSeconds;

    public void setUsername(String username) {
        this.username = username;
        System.out.println("StudyTimerController: setUsername called with: " + username); // Debugging print
        this.USER_RECORDS_FILE = BASE_DATA_PATH + username + "_study_records.txt";
        this.USER_COURSES_FILE = BASE_DATA_PATH + "courses_" + username + ".txt";
        System.out.println("StudyTimerController: USER_COURSES_FILE set to: " + USER_COURSES_FILE); // Debugging print

        try {
            // Ensure the data directory exists
            Files.createDirectories(Paths.get(BASE_DATA_PATH)); // Use Files.createDirectories for robustness
        } catch (IOException e) {
            showAlert("Error", "Could not create data directory: " + e.getMessage());
            e.printStackTrace();
        }

        // Call load and populate methods here, after username and file paths are set
        loadUserCoursesAndTopics();
        loadBestStudyTimes();
        populateCourseComboBox();
    }

    @FXML
    public void initialize() {
        userCoursesAndTopics = new HashMap<>();
        bestStudyTimesSeconds = new HashMap<>();

        // Digital Timer Timeline: updates every second
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            if (seconds == 60) {
                minutes++;
                seconds = 0;
            }
            if (minutes == 60) {
                hours++;
                minutes = 0;
            }
            updateTimerLabel();
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);

        // Animation Timer for smooth circular progress
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long nowNanos) {
                // Calculate elapsed time within the current animation cycle
                double elapsedNanos = nowNanos - animationStartTimeNanos;
                double progress = (elapsedNanos / 1_000_000_000.0) / ANIMATION_CYCLE_SECONDS; // Progress from 0.0 to 1.0 within one animation cycle
                drawCircularProgress(progress);
            }
        };


        courseSelectionComboBox.valueProperty().addListener((obs, oldVal, newCourseName) -> {
            if (newCourseName != null) {
                populateTopicComboBox(newCourseName);
                updateSelectedCourseTopicLabel();
            } else {
                topicSelectionComboBox.getItems().clear();
                topicSelectionComboBox.setPromptText("Select Topic");
                selectedCourseTopicLabel.setText("No course selected");
            }
        });

        topicSelectionComboBox.valueProperty().addListener((obs, oldVal, newTopicName) -> {
            updateSelectedCourseTopicLabel();
        });

        if (calendarNavBtn != null) calendarNavBtn.setOnAction(this::goToCalendar);
        if (courseTrackerNavBtn != null) courseTrackerNavBtn.setOnAction(this::goToCourseTracker);
        if (timerNavBtn != null) timerNavBtn.setOnAction(this::goToStudyTimer);
        if (dashboardNavBtn != null) dashboardNavBtn.setOnAction(this::goToDashboard);
        if (communityBtn != null) communityBtn.setOnAction(this::goToCommunity);

        updateTimerLabel();
        updateSelectedCourseTopicLabel();
        drawCircularProgress(0.0); // Initial draw of the circular timer at 0 progress
        // The initial loading and population now happens in setUsername,
        // which should be called externally after the FXML is loaded and username is available.
    }

    private void loadUserCoursesAndTopics() {
        userCoursesAndTopics.clear();
        Path filePath = Paths.get(USER_COURSES_FILE);
        System.out.println("StudyTimerController: Attempting to load courses from: " + filePath.toAbsolutePath()); // Debugging print


        if (Files.exists(filePath)) {
            System.out.println("StudyTimerController: Course file found: " + filePath.toAbsolutePath()); // Debugging print
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("StudyTimerController: Reading line: " + line); // Debugging print
                    Course course = Course.fromCsvString(line);
                    if (course != null) {
                        userCoursesAndTopics.put(course.getCourseName(), course.getTopics());
                        System.out.println("StudyTimerController: Added course: " + course.getCourseName() + " with topics: " + course.getTopics()); // Debugging print
                    } else {
                        System.err.println("StudyTimerController: Failed to parse course from line: " + line); // Debugging print
                    }
                }
                if (userCoursesAndTopics.isEmpty()) {
                    System.out.println("StudyTimerController: No courses parsed from file, map is empty."); // Debugging print
                }
            } catch (IOException e) {
                showAlert("Error", "Error loading user courses: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("StudyTimerController: No course data file found for user " + username + " at " + USER_COURSES_FILE + ".");
        }
    }

    private void populateCourseComboBox() {
        ObservableList<String> courseNames = FXCollections.observableArrayList(userCoursesAndTopics.keySet());
        courseSelectionComboBox.setItems(courseNames);
        System.out.println("StudyTimerController: Populating course combo box with: " + courseNames); // Debugging print

        if (courseNames.isEmpty()) {
            // Show message and disable controls if no courses are found
            noCoursesMessageLabel.setVisible(true);
            noCoursesMessageLabel.setManaged(true);
            courseSelectionComboBox.setDisable(true);
            topicSelectionComboBox.setDisable(true);
            startButton.setDisable(true);
            stopButton.setDisable(true);
            resetButton.setDisable(true);
            selectedCourseTopicLabel.setText("No courses available to study.");
        } else {
            // Hide message and enable controls if courses are found
            noCoursesMessageLabel.setVisible(false);
            noCoursesMessageLabel.setManaged(false);
            courseSelectionComboBox.setDisable(false);
            topicSelectionComboBox.setDisable(false);
            startButton.setDisable(false);
            stopButton.setDisable(false);
            resetButton.setDisable(false);

            courseSelectionComboBox.getSelectionModel().selectFirst();
            System.out.println("StudyTimerController: Selected first course: " + courseSelectionComboBox.getSelectionModel().getSelectedItem()); // Debugging print
        }
    }

    private void populateTopicComboBox(String courseName) {
        List<String> topics = userCoursesAndTopics.get(courseName);
        System.out.println("StudyTimerController: Populating topic combo box for course: " + courseName + " with topics: " + topics); // Debugging print
        if (topics != null) {
            ObservableList<String> topicNames = FXCollections.observableArrayList(topics);
            topicSelectionComboBox.setItems(topicNames);
            if (!topicNames.isEmpty()) {
                topicSelectionComboBox.getSelectionModel().selectFirst();
            } else {
                topicSelectionComboBox.getSelectionModel().clearSelection();
                topicSelectionComboBox.setPromptText("No topics available");
            }
        } else {
            topicSelectionComboBox.getItems().clear();
            topicSelectionComboBox.setPromptText("Select Topic");
        }
    }

    private void updateSelectedCourseTopicLabel() {
        String selectedCourse = courseSelectionComboBox.getSelectionModel().getSelectedItem();
        String selectedTopic = topicSelectionComboBox.getSelectionModel().getSelectedItem();

        if (selectedCourse != null && selectedTopic != null) {
            selectedCourseTopicLabel.setText("Studying: " + selectedTopic + " from " + selectedCourse);
        } else if (selectedCourse != null) {
            selectedCourseTopicLabel.setText("Selected Course: " + selectedCourse + " (No topic selected)");
        } else {
            selectedCourseTopicLabel.setText("No course/topic selected");
        }
    }

    private void loadBestStudyTimes() {
        bestStudyTimesSeconds.clear();
        Path filePath = Paths.get(USER_RECORDS_FILE);
        System.out.println("StudyTimerController: Attempting to load best study times from: " + filePath.toAbsolutePath()); // Debugging print


        if (Files.exists(filePath)) {
            System.out.println("StudyTimerController: Study records file found: " + filePath.toAbsolutePath()); // Debugging print
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("StudyTimerController: Reading record line: " + line); // Debugging print
                    // Format: username,timestamp,time,courseName,topicName
                    String[] parts = line.split(",");
                    if (parts.length == 5) {
                        String topicName = parts[4];
                        String timeString = parts[2]; // HH:mm:ss
                        int durationInSeconds = parseTimeToSeconds(timeString);

                        // Update best time if current record is longer
                        bestStudyTimesSeconds.merge(topicName, durationInSeconds, Math::max);
                        System.out.println("StudyTimerController: Updated best time for " + topicName + ": " + durationInSeconds + " seconds."); // Debugging print
                    } else {
                        System.err.println("StudyTimerController: Malformed record line: " + line); // Debugging print
                    }
                }
            } catch (IOException e) {
                showAlert("Error", "Error loading best study times: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("StudyTimerController: Study records file NOT found: " + filePath.toAbsolutePath()); // Debugging print
        }
    }

    private void saveRecord(String time, String courseName, String topicName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String record = String.format("%s,%s,%s,%s,%s", username, timestamp, time, courseName, topicName);
        System.out.println("StudyTimerController: Saving record: " + record + " to " + USER_RECORDS_FILE); // Debugging print

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(USER_RECORDS_FILE), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(record);
            writer.newLine();

            // Check for new record
            int currentSessionSeconds = parseTimeToSeconds(time);
            int previousBestSeconds = bestStudyTimesSeconds.getOrDefault(topicName, 0);

            if (currentSessionSeconds > previousBestSeconds) {
                bestStudyTimesSeconds.put(topicName, currentSessionSeconds);
                showAlert("Congratulations!", "New record for " + topicName + "! You studied for " + time + "!");
            }

        } catch (IOException e) {
            showAlert("Error", "Unable to save record.");
            e.printStackTrace();
        }
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

    @FXML
    private void startButton(ActionEvent event) {
        String selectedCourse = courseSelectionComboBox.getSelectionModel().getSelectedItem();
        String selectedTopic = topicSelectionComboBox.getSelectionModel().getSelectedItem();

        if (selectedCourse == null || selectedTopic == null) {
            showAlert("Selection Error", "Please select both a course and a topic to start the timer.");
            return;
        }

        if (!running) {
            timerTimeline.play(); // Start digital timer
            animationStartTimeNanos = System.nanoTime(); // Record start time for animation
            animationTimer.start(); // Start visual animation
            running = true;
            // Change icon to pause
            if (startIcon != null) {
                startIcon.setIconLiteral("fas-pause");
            }
        } else {
            timerTimeline.pause(); // Pause digital timer
            animationTimer.stop(); // Stop visual animation
            running = false;
            // Change icon to resume
            if (startIcon != null) {
                startIcon.setIconLiteral("fas-play"); // Or "fas-play" if you want play/pause toggle
            }
        }
    }

    @FXML
    private void stopButton(ActionEvent event) {
        if (running) {
            timerTimeline.stop(); // Stop digital timer
            animationTimer.stop(); // Stop visual animation
            running = false;
            if (startIcon != null) {
                startIcon.setIconLiteral("fas-play"); // Reset to play icon
            }

            String selectedCourse = courseSelectionComboBox.getSelectionModel().getSelectedItem();
            String selectedTopic = topicSelectionComboBox.getSelectionModel().getSelectedItem();

            if (selectedCourse == null || selectedTopic == null) {
                showAlert("Error", "No course or topic was selected for this study session.");
                return;
            }

            String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            saveRecord(time, selectedCourse, selectedTopic);
            showAlert("Session Ended", "Your study session for " + selectedTopic + " from " + selectedCourse + " (" + time + ") has been recorded.");
            resetTimer(); // Resets time variables
            drawCircularProgress(0.0); // Redraws the canvas to show 0 progress
        } else {
            showAlert("Info", "Timer is not running.");
        }
    }

    @FXML
    private void resetButton(ActionEvent event) {
        timerTimeline.stop(); // Stop digital timer
        animationTimer.stop(); // Stop visual animation
        running = false;
        hours = 0;
        minutes = 0;
        seconds = 0;
        updateTimerLabel();
        drawCircularProgress(0.0); // Reset the circular progress to 0
        if (startIcon != null) {
            startIcon.setIconLiteral("fas-play"); // Reset to play icon
        }
    }

    private void updateTimerLabel() {
        if (timerLabel != null) {
            timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        }
    }

    // Method to draw the circular progress, now taking a progress value
    private void drawCircularProgress(double currentAnimationProgress) {
        if (timerCanvas == null) {
            return; // Ensure canvas is initialized
        }

        GraphicsContext gc = timerCanvas.getGraphicsContext2D();
        double width = timerCanvas.getWidth();
        double height = timerCanvas.getHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        // Adjusted radius to fit well within the 200x200 StackPane and provide padding
        double radius = Math.min(width, height) / 2 - 20; // Adjusted radius for better fit

        // Clear the canvas
        gc.clearRect(0, 0, width, height);

        // Draw the background circle (dark grey)
        gc.setStroke(Color.rgb(50, 50, 50)); // Dark grey for background circle
        gc.setLineWidth(15); // Thicker line for the circle
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // Calculate arc extent based on the continuous animation progress
        // currentAnimationProgress will go from 0.0 to 1.0 repeatedly
        double startAngle = 90; // Start from the top (12 o'clock)
        double arcExtent = -360 * (currentAnimationProgress % 1.0); // Use modulo to ensure it wraps around

        // Draw the animated arc (purple)
        gc.setStroke(Color.rgb(153, 51, 255)); // Purple color for the progress arc
        gc.setLineWidth(15); // Match background circle line width
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle, arcExtent, javafx.scene.shape.ArcType.OPEN);
    }


    // Removed showPreviousRecords method from here as per user's request

    private void resetTimer() {
        hours = 0;
        minutes = 0;
        seconds = 0;
        updateTimerLabel();
    }

    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
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
            showAlert("Navigation Error", "Could not load Calendar page.");
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
            showAlert("Navigation Error", "Could not load Course Tracker page.");
        }
    }

    @FXML
    private void goToStudyTimer(ActionEvent event) {
        System.out.println("Already on Study Timer page.");
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
            showAlert("Navigation Error", "Could not load Dashboard page.");
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
            showAlert("Navigation Error", "Could not load Community page.");
        }
    }
}
