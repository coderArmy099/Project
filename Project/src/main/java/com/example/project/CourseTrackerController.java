package com.example.project;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Platform;

public class CourseTrackerController {

    /* --------------- FXML refs --------------- */
    @FXML private BorderPane rootPane;
    @FXML private VBox navPane;
    @FXML private Button calendarBtn;
    @FXML private Button courseTrackerNavBtn;
    @FXML private Button timerNavBtn;
    @FXML private Button dashboardBtn;
    @FXML private Button communityBtn;

    @FXML private StackPane rootStack;
    @FXML private FlowPane courseFlowPane;
    @FXML private Label noCoursesPromptLabel;
    @FXML private Button addCourseBtn;


    // Add Course Form elements
    @FXML private VBox addCoursePane;
    @FXML private TextField courseNameField;
    @FXML private TextField teacherNameField;
    @FXML private Spinner<Integer> creditHourSpinner;
    @FXML private TextField newTopicField;
    @FXML private Button addTopicBtn;
    @FXML private FlowPane topicsDisplayBox;
    @FXML private TextField refBooksField;
    @FXML private Button cancelBtn;
    @FXML private Button createBtn;

    // Course Details elements
    @FXML private VBox courseDetailsPane;
    @FXML private Label detailCourseNameLabel;
    @FXML private Canvas detailProgressCanvas;
    @FXML private Label detailProgressText;
    @FXML private VBox detailTopicsChecklist;
    @FXML private Button closeDetailsBtn;
    @FXML private Button okDetailsBtn;
    @FXML private Button showStudyRecordsBtn;
    // Removed studyTimeBarGraphCanvas from here

    // Custom Study Records Display elements
    @FXML private VBox studyRecordsDisplayPane;
    @FXML private Label studyRecordsTitleLabel;
    @FXML private VBox studyRecordsContentBox;
    @FXML private Button closeStudyRecordsBtn;
    @FXML private Canvas studyTimeBarGraphCanvas; // Moved FXML ref for the new bar graph canvas here

    // Custom Confirmation Dialog elements
    @FXML private VBox customConfirmDialog;
    @FXML private Label confirmMessageLabel;
    @FXML private Button confirmYesButton;
    @FXML private Button confirmNoButton;


    private final String BASE_DATA_PATH = "data/";
    private String USER_RECORDS_FILE;
    private String USER_COURSES_FILE;
    private ObservableList<Course> courses;
    private List<String> newCourseTopics;

    private String username;
    private Course courseToDelete; // To hold the course being confirmed for deletion
    private Course currentCourseForRecords; // To hold the course whose records are being displayed

    public void setUsername(String username) {
        this.username = username;
        try {
            Files.createDirectories(Paths.get(BASE_DATA_PATH));
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not create data directory: " + e.getMessage());
            e.printStackTrace();
        }
        this.USER_COURSES_FILE = BASE_DATA_PATH + "courses_" + username + ".txt";
        this.USER_RECORDS_FILE = BASE_DATA_PATH + username + "_study_records.txt";
        loadCourses();
        populateCourseCards();
    }

    @FXML
    public void initialize() {
        courses = FXCollections.observableArrayList();
        newCourseTopics = new ArrayList<>();

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3);
        creditHourSpinner.setValueFactory(valueFactory);

        if (addCourseBtn != null) addCourseBtn.setOnAction(this::showAddCourseForm);
        if (cancelBtn != null) cancelBtn.setOnAction(this::hideAddCourseForm);
        if (createBtn != null) createBtn.setOnAction(this::createCourse);
        if (addTopicBtn != null) addTopicBtn.setOnAction(this::addTopicToNewCourse);
        if (okDetailsBtn != null) okDetailsBtn.setOnAction(e -> hideCourseDetails());
        if (showStudyRecordsBtn != null) showStudyRecordsBtn.setOnAction(this::showStudyRecordsForCourse);

        if (closeStudyRecordsBtn != null) closeStudyRecordsBtn.setOnAction(e -> hideStudyRecordsDisplay());

        // Set actions for custom confirmation dialog buttons
        if (confirmYesButton != null) confirmYesButton.setOnAction(e -> handleConfirmDialog(true));
        if (confirmNoButton != null) confirmNoButton.setOnAction(e -> handleConfirmDialog(false));


        if (calendarBtn != null) calendarBtn.setOnAction(this::goToCalendar);
        if (timerNavBtn != null) timerNavBtn.setOnAction(this::goToStudyTimer);
        if (dashboardBtn != null) dashboardBtn.setOnAction(this::goToDashboard);
    }

    private void loadCourses() {
        courses.clear();
        if (USER_COURSES_FILE == null) {
            System.err.println("USER_COURSES_FILE is null. Cannot load courses.");
            return;
        }
        Path filePath = Paths.get(USER_COURSES_FILE);
        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Course course = Course.fromCsvString(line);
                    if (course != null) {
                        courses.add(course);
                    }
                }
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Error loading courses: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No course data file found for user: " + username);
        }
    }

    private void saveCourses() {
        if (USER_COURSES_FILE == null) {
            System.err.println("USER_COURSES_FILE is null. Cannot save courses.");
            return;
        }
        Path filePath = Paths.get(USER_COURSES_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Course course : courses) {
                writer.write(course.toCsvString());
                writer.newLine();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error saving courses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateCourseCards() {
        courseFlowPane.getChildren().clear();
        if (courses.isEmpty()) {
            noCoursesPromptLabel.setVisible(true);
            noCoursesPromptLabel.setManaged(true);
        } else {
            noCoursesPromptLabel.setVisible(false);
            noCoursesPromptLabel.setManaged(false);
            for (Course course : courses) {
                courseFlowPane.getChildren().add(createCourseCard(course));
            }
        }
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox(10);
        card.getStyleClass().add("course-card");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_LEFT);

        // Make the entire card clickable to show details
        card.setOnMouseClicked(e -> showCourseDetails(course));

        Label courseNameLabel = new Label(course.getCourseName());
        courseNameLabel.getStyleClass().add("course-title-label"); // Styled with #00bfa6 color via CSS

        Label teacherNameLabel = new Label("Teacher: " + course.getTeacherName());
        teacherNameLabel.getStyleClass().add("course-detail-label");

        Label creditHourLabel = new Label("Credits: " + course.getCreditHour());
        creditHourLabel.getStyleClass().add("course-detail-label");

        ProgressBar progressBar = new ProgressBar(course.getProgress());
        progressBar.setPrefWidth(200); // Set a preferred width
        progressBar.getStyleClass().add("course-progress-bar"); // Add a style class for custom styling
        Label progressText = new Label(String.format("%.0f%%", course.getProgress() * 100));
        progressText.getStyleClass().add("accent-label");

        HBox progressBox = new HBox(5, progressBar, progressText);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(progressBox, Priority.ALWAYS); // Push progress bar to bottom if needed

        // Delete Icon at top right
        Button deleteIconBtn = new Button();
        FontIcon deleteIcon = new FontIcon("fas-times"); // FontAwesome cross icon
        deleteIcon.getStyleClass().add("delete-icon"); // Style the icon
        deleteIconBtn.setGraphic(deleteIcon);
        deleteIconBtn.getStyleClass().add("icon-button"); // Style the button transparently
        deleteIconBtn.setOnAction(e -> {
            e.consume(); // Consume the event to prevent card click
            confirmDeleteCourse(course);
        });

        StackPane topArea = new StackPane();
        topArea.getChildren().addAll(courseNameLabel);
        StackPane.setAlignment(courseNameLabel, Pos.TOP_LEFT);
        StackPane.setAlignment(deleteIconBtn, Pos.TOP_RIGHT); // Position delete icon top right
        topArea.getChildren().add(deleteIconBtn);
        StackPane.setMargin(deleteIconBtn, new Insets(-5, -5, 0, 0)); // Adjust margin to push it to corner

        card.getChildren().addAll(topArea, teacherNameLabel, creditHourLabel, progressBox);
        return card;
    }

    private void showAddCourseForm(ActionEvent event) {
        courseNameField.clear();
        teacherNameField.clear();
        creditHourSpinner.getValueFactory().setValue(3);
        newTopicField.clear();
        topicsDisplayBox.getChildren().clear();
        newCourseTopics.clear();
        refBooksField.clear();

        addCoursePane.setVisible(true);
        addCoursePane.setManaged(true);
    }

    private void hideAddCourseForm(ActionEvent event) {
        addCoursePane.setVisible(false);
        addCoursePane.setManaged(false);
    }

    private void createCourse(ActionEvent event) {
        String courseName = courseNameField.getText().trim();
        String teacherName = teacherNameField.getText().trim();
        int creditHour = creditHourSpinner.getValue();
        String referenceBooks = refBooksField.getText().trim();

        if (courseName.isEmpty() || teacherName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter course name and teacher name.");
            return;
        }

        Course newCourse = new Course(courseName, teacherName, creditHour, newCourseTopics, referenceBooks);
        courses.add(newCourse);
        saveCourses();
        populateCourseCards();
        hideAddCourseForm(event);
        showAlert(Alert.AlertType.INFORMATION, "Success", "Course '" + courseName + "' added successfully!");
    }

    private void addTopicToNewCourse(ActionEvent event) {
        String topic = newTopicField.getText().trim();
        if (!topic.isEmpty() && !newCourseTopics.contains(topic)) {
            newCourseTopics.add(topic);
            updateTopicsDisplay();
            newTopicField.clear();
        } else if (topic.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Topic", "Please enter a topic to add.");
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Duplicate Topic", "'" + topic + "' is already in the list.");
        }
    }

    private void updateTopicsDisplay() {
        topicsDisplayBox.getChildren().clear();
        for (String topic : newCourseTopics) {
            HBox topicItem = new HBox(5);
            topicItem.setAlignment(Pos.CENTER_LEFT);
            topicItem.setStyle("-fx-background-color: #4a4a4a; -fx-background-radius: 5; -fx-padding: 5;");

            Label topicLabel = new Label(topic);
            topicLabel.setStyle("-fx-text-fill: #e0e0e0;");

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().add("flat-btn");
            removeButton.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff6666;");
            removeButton.setOnAction(e -> {
                newCourseTopics.remove(topic);
                updateTopicsDisplay();
            });
            topicItem.getChildren().addAll(topicLabel, removeButton);
            topicsDisplayBox.getChildren().add(topicItem);
        }
    }

    private void showCourseDetails(Course course) {
        currentCourseForRecords = course; // Set the current course for study records

        detailCourseNameLabel.setText(course.getCourseName());
        detailProgressText.setText(String.format("%.0f%%", course.getProgress() * 100));
        detailProgressText.setTextFill(Color.WHITE);
        drawPieChartProgress(course.getProgress());

        // Set font sizes for headings
        // Using lookup to get the specific Label nodes and set their font
        Node progressLabelNode = courseDetailsPane.lookup("Label[text='Progress:']");
        if (progressLabelNode instanceof Label) {
            ((Label) progressLabelNode).setFont(new Font("System Bold", 18));
        }
        Node topicsChecklistLabelNode = courseDetailsPane.lookup("Label[text='Topics Checklist:']");
        if (topicsChecklistLabelNode instanceof Label) {
            ((Label) topicsChecklistLabelNode).setFont(new Font("System Bold", 18));
        }
        // Removed "Study Time Breakdown" heading from here as graph is moved

        detailTopicsChecklist.getChildren().clear();
        for (String topic : course.getTopics()) {
            CheckBox checkBox = new CheckBox(topic);
            checkBox.setSelected(course.getCompletedTopics().contains(topic));
            checkBox.getStyleClass().add("check-box"); // Apply CSS for checkbox text
            checkBox.setOnAction(e -> {
                if (checkBox.isSelected()) {
                    course.markTopicCompleted(topic);
                } else {
                    course.markTopicIncomplete(topic);
                }
                course.calculateProgress();
                detailProgressText.setText(String.format("%.0f%%", course.getProgress() * 100));
                drawPieChartProgress(course.getProgress());
                saveCourses();
                populateCourseCards();
            });
            detailTopicsChecklist.getChildren().add(checkBox);
        }

        courseDetailsPane.setVisible(true);
        courseDetailsPane.setManaged(true);
        studyRecordsDisplayPane.setVisible(false);
        studyRecordsDisplayPane.setManaged(false);
    }

    private void hideCourseDetails() {
        courseDetailsPane.setVisible(false);
        courseDetailsPane.setManaged(false);
    }

    private void drawPieChartProgress(double progress) {
        if (detailProgressCanvas == null) {
            return;
        }

        GraphicsContext gc = detailProgressCanvas.getGraphicsContext2D();
        double width = detailProgressCanvas.getWidth();
        double height = detailProgressCanvas.getHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) / 2 - 5;

        gc.clearRect(0, 0, width, height);

        gc.setFill(Color.rgb(50, 50, 50));
        gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        gc.setFill(Color.rgb(0, 191, 166));
        double startAngle = 90;
        double arcExtent = -360 * progress;

        gc.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle, arcExtent, javafx.scene.shape.ArcType.ROUND);
    }

    private Map<String, Integer> getStudyTimesForCourseTopics(String courseName) {
        Map<String, Integer> topicStudyTimes = new HashMap<>();
        Path recordFilePath = Paths.get(USER_RECORDS_FILE);

        try {
            if (Files.exists(recordFilePath) && Files.size(recordFilePath) > 0) {
                try (BufferedReader reader = Files.newBufferedReader(recordFilePath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length == 5) {
                            String recordUsername = parts[0];
                            String time = parts[2];
                            String course = parts[3];
                            String topic = parts[4];

                            if (recordUsername.equals(username) && course.equals(courseName)) {
                                int durationInSeconds = parseTimeToSeconds(time);
                                topicStudyTimes.merge(topic, durationInSeconds, Integer::sum);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading study records for bar graph: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return topicStudyTimes;
    }


    private void drawBarGraph(Map<String, Integer> topicStudyTimes) {
        if (studyTimeBarGraphCanvas == null) {
            return;
        }

        GraphicsContext gc = studyTimeBarGraphCanvas.getGraphicsContext2D();
        // Fill the entire canvas with the dark background color
        gc.setFill(Color.web("#2b2b2b"));
        gc.fillRect(0, 0, studyTimeBarGraphCanvas.getWidth(), studyTimeBarGraphCanvas.getHeight());


        double canvasWidth = studyTimeBarGraphCanvas.getWidth();
        double canvasHeight = studyTimeBarGraphCanvas.getHeight();
        double padding = 20; // Padding from canvas edges
        double barSpacing = 15; // Space between bars
        double labelAreaHeight = 60; // Increased height for topic and time labels below and above bars

        List<Map.Entry<String, Integer>> sortedTopics = topicStudyTimes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        if (sortedTopics.isEmpty()) {
            gc.setFill(Color.web("#b0b0b0"));
            gc.setFont(new Font("System Bold", 16));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("No study data for topics yet.", canvasWidth / 2, canvasHeight / 2);
            return;
        }

        // Find max study time for scaling
        int maxTime = sortedTopics.stream()
                .mapToInt(Map.Entry::getValue)
                .max()
                .orElse(0);

        if (maxTime == 0) {
            maxTime = 1; // Avoid division by zero
        }

        double minBarWidth = 60; // Minimum width for each bar to ensure readability
        double requiredWidth = sortedTopics.size() * minBarWidth + (sortedTopics.size() - 1) * barSpacing + 2 * padding;

        // Dynamically set canvas width to accommodate all bars
        // This needs to be done on the JavaFX Application Thread
        int finalMaxTime = maxTime;
        Platform.runLater(() -> {
            // Get the actual width of the parent (ScrollPane's viewport)
            double parentWidth = studyTimeBarGraphCanvas.getParent().getBoundsInLocal().getWidth();
            studyTimeBarGraphCanvas.setWidth(Math.max(parentWidth, requiredWidth));
            // Redraw after resizing
            drawBarGraphContent(gc, topicStudyTimes, sortedTopics, finalMaxTime, padding, barSpacing, labelAreaHeight, minBarWidth);
        });
    }

    // Helper method to draw content after canvas resize
    private void drawBarGraphContent(GraphicsContext gc, Map<String, Integer> topicStudyTimes, List<Map.Entry<String, Integer>> sortedTopics, int maxTime, double padding, double barSpacing, double labelAreaHeight, double minBarWidth) {
        gc.clearRect(0, 0, studyTimeBarGraphCanvas.getWidth(), studyTimeBarGraphCanvas.getHeight()); // Clear again after resize
        gc.setFill(Color.web("#2b2b2b")); // Fill background again
        gc.fillRect(0, 0, studyTimeBarGraphCanvas.getWidth(), studyTimeBarGraphCanvas.getHeight());

        double canvasWidth = studyTimeBarGraphCanvas.getWidth();
        double canvasHeight = studyTimeBarGraphCanvas.getHeight();

        double usableWidth = canvasWidth - 2 * padding;
        double barWidth = (usableWidth - (sortedTopics.size() - 1) * barSpacing) / sortedTopics.size();
        barWidth = Math.max(barWidth, minBarWidth);

        double usableHeight = canvasHeight - 2 * padding - labelAreaHeight;
        if (usableHeight <= 0) usableHeight = canvasHeight / 2;

        // Graph content will always start from the left padding.
        double currentX = padding;


        gc.setTextAlign(TextAlignment.CENTER);

        // Draw X-axis (bottom line)
        double xAxisY = canvasHeight - padding - labelAreaHeight / 2;
        gc.setStroke(Color.web("#e0e0e0"));
        gc.setLineWidth(1);
        gc.strokeLine(currentX, xAxisY, currentX + (sortedTopics.size() * barWidth) + ((sortedTopics.size() - 1) * barSpacing) + 5, xAxisY); // Extend for arrow

        // Draw Y-axis (left line)
        double yAxisX = currentX;
        gc.strokeLine(yAxisX, padding - 5, yAxisX, canvasHeight - padding - labelAreaHeight / 2); // Extend for arrow

        // Draw X-axis arrow
        gc.strokeLine(currentX + (sortedTopics.size() * barWidth) + ((sortedTopics.size() - 1) * barSpacing) + 5, xAxisY, currentX + (sortedTopics.size() * barWidth) + ((sortedTopics.size() - 1) * barSpacing) - 5, xAxisY - 5);
        gc.strokeLine(currentX + (sortedTopics.size() * barWidth) + ((sortedTopics.size() - 1) * barSpacing) + 5, xAxisY, currentX + (sortedTopics.size() * barWidth) + ((sortedTopics.size() - 1) * barSpacing) - 5, xAxisY + 5);

        // Draw Y-axis arrow
        gc.strokeLine(yAxisX, padding - 5, yAxisX - 5, padding + 5);
        gc.strokeLine(yAxisX, padding - 5, yAxisX + 5, padding + 5);

        // Draw Y-axis label "Study Time"
        gc.save(); // Save current state
        // Adjust translate coordinates to position the label clearly to the left of the Y-axis
        // The x-coordinate for translation should be to the left of the yAxisX, by an amount that centers the rotated text.
        // The y-coordinate for translation should be the center of the usable height for the graph.
        gc.translate(yAxisX - 40, padding + usableHeight / 2); // Adjusted X-coordinate and Y-coordinate to align with the middle of the graph bars
        gc.rotate(-90); // Rotate to draw vertically
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Study Time", 0, 0); // Draw label at the new origin (0,0)
        gc.restore(); // Restore original state


        for (Map.Entry<String, Integer> entry : sortedTopics) {
            String topic = entry.getKey();
            int timeInSeconds = entry.getValue();
            double barHeight = (double) timeInSeconds / maxTime * usableHeight;

            // Draw bar
            gc.setFill(Color.web("#674ea7")); // Changed to requested color
            gc.fillRect(currentX, canvasHeight - padding - labelAreaHeight / 2 - barHeight, barWidth, barHeight);

            // Draw topic label (individual topic names)
            gc.setFill(Color.web("#e0e0e0")); // Light grey for topic labels
            gc.setFont(new Font("System", 12)); // Fixed font size for topic labels
            String displayTopic = topic;
            // Simple truncation if topic name is too long for the bar width
            javafx.scene.text.Text textMeasurer = new javafx.scene.text.Text(topic);
            textMeasurer.setFont(new Font("System", 12));
            double textWidth = textMeasurer.getLayoutBounds().getWidth();

            if (textWidth > barWidth - 5) { // Allow a small margin
                int maxChars = (int) ((barWidth - 5) / (12 * 0.6)); // Estimate chars that fit for font size 12
                if (maxChars > 3) {
                    displayTopic = topic.substring(0, Math.min(topic.length(), maxChars - 3)) + "...";
                } else {
                    displayTopic = ""; // Too small to show anything meaningful
                }
            }
            gc.fillText(displayTopic, currentX + barWidth / 2, canvasHeight - padding + 15); // Position below bar

            // Draw time label (converted to HH:mm:ss or mm:ss)
            if (barHeight > 20) { // Only show time label if bar is tall enough
                String timeFormatted = formatSecondsToHMS(timeInSeconds);
                gc.setFill(Color.web("#e0e0e0"));
                gc.setFont(new Font("System", 10)); // Fixed font size for time labels
                gc.fillText(timeFormatted, currentX + barWidth / 2, canvasHeight - padding - labelAreaHeight / 2 - barHeight - 5); // Position above bar
            }

            currentX += barWidth + barSpacing;
        }

        // Draw X-axis label "Topics"
        gc.setFill(Color.web("#e0e0e0"));
        gc.setFont(new Font("System Bold", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        // Position X-axis label clearly below the individual topic names
        gc.fillText("Topics", padding + (sortedTopics.size() * barWidth + (sortedTopics.size() - 1) * barSpacing) / 2, canvasHeight - padding + 45); // Adjusted Y-coordinate to be even lower
    }


    private String formatSecondsToHMS(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }


    private void showStudyRecordsForCourse(ActionEvent event) {
        // Use currentCourseForRecords to get the course name
        if (currentCourseForRecords == null) {
            showAlert(Alert.AlertType.WARNING, "No Course Selected", "Please view details of a course first.");
            return;
        }

        studyRecordsTitleLabel.setText("Study Records for " + currentCourseForRecords.getCourseName());
        studyRecordsContentBox.getChildren().clear();

        Path recordFilePath = Paths.get(USER_RECORDS_FILE);

        List<StudyRecordEntry> courseRecords = new ArrayList<>();

        try {
            if (Files.exists(recordFilePath) && Files.size(recordFilePath) > 0) {
                try (BufferedReader reader = Files.newBufferedReader(recordFilePath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length == 5) {
                            String recordUsername = parts[0];
                            String timestamp = parts[1];
                            String time = parts[2];
                            String course = parts[3];
                            String topic = parts[4];

                            if (recordUsername.equals(username) && course.equals(currentCourseForRecords.getCourseName())) {
                                courseRecords.add(new StudyRecordEntry(timestamp, time, course, topic));
                            }
                        }
                    }
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Error reading study records: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (courseRecords.isEmpty()) {
            Label noRecordsLabel = new Label("No study records found for this course.");
            noRecordsLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14px;");
            studyRecordsContentBox.getChildren().add(noRecordsLabel);
        } else {
            courseRecords.sort((r1, r2) -> Integer.compare(parseTimeToSeconds(r2.getDuration()), parseTimeToSeconds(r1.getDuration())));

            for (StudyRecordEntry record : courseRecords) {
                Label recordLabel = new Label(
                        "Topic: " + record.getTopic() +
                                ", Date: " + record.getTimestamp() +
                                ", Duration: " + record.getDuration()
                );
                recordLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-padding: 2 0 2 0;");
                studyRecordsContentBox.getChildren().add(recordLabel);
            }
        }

        // Draw the bar graph in the study records pane
        Map<String, Integer> topicStudyTimes = getStudyTimesForCourseTopics(currentCourseForRecords.getCourseName());
        drawBarGraph(topicStudyTimes);

        // Set font size for "Study Time Breakdown" heading in study records pane
        Node studyTimeBreakdownLabelNode = studyRecordsDisplayPane.lookup("Label[text='Study Time Breakdown:']");
        if (studyTimeBreakdownLabelNode instanceof Label) {
            ((Label) studyTimeBreakdownLabelNode).setFont(new Font("System Bold", 18));
        }


        courseDetailsPane.setVisible(false);
        courseDetailsPane.setManaged(false);
        studyRecordsDisplayPane.setVisible(true);
        studyRecordsDisplayPane.setManaged(true);
    }

    private void hideStudyRecordsDisplay() {
        studyRecordsDisplayPane.setVisible(false);
        studyRecordsDisplayPane.setManaged(false);
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

    private static class StudyRecordEntry {
        private String timestamp;
        private String duration;
        private String course;
        private String topic;

        public StudyRecordEntry(String timestamp, String duration, String course, String topic) {
            this.timestamp = timestamp;
            this.duration = duration;
            this.course = course;
            this.topic = topic;
        }

        public String getTimestamp() { return timestamp; }
        public String getDuration() { return duration; }
        public String getCourse() { return course; }
        public String getTopic() { return topic; }
    }


    private void confirmDeleteCourse(Course course) {
        this.courseToDelete = course; // Store the course to be deleted
        confirmMessageLabel.setText("Are you sure you want to delete the course '" + course.getCourseName() + "'? This action cannot be undone.");
        customConfirmDialog.setVisible(true);
        customConfirmDialog.setManaged(true);
    }

    private void handleConfirmDialog(boolean confirmed) {
        customConfirmDialog.setVisible(false);
        customConfirmDialog.setManaged(false);

        if (confirmed && courseToDelete != null) {
            courses.remove(courseToDelete);
            saveCourses();
            populateCourseCards();
            showAlert(Alert.AlertType.INFORMATION, "Deleted", "'" + courseToDelete.getCourseName() + "' has been deleted.");
            courseToDelete = null; // Clear the stored course
        } else {
            courseToDelete = null; // Clear the stored course even if not confirmed
        }
    }


    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Navigation methods
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
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could could not load Study Timer page.");
        }
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
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load Dashboard page.");
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
}
