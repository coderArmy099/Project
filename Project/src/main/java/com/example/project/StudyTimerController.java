package com.example.project;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StudyTimerController {
    private String username;
    @FXML
    private Label timerLabel;
    @FXML
    private Button Back;
    @FXML
    private Button Start;

    @FXML
    private Button Stop;

    @FXML
    private Button Reset;

    @FXML
    private Button showPreviousButton;

    private Timeline timeline;
    private int hours = 0, minutes = 0, seconds = 0;
    private boolean running = false;
    private final String RECORD_FILE = "study_records.txt";

    public void setUsername(String username) {
        this.username = username;
    }

    @FXML
    public void initialize() {
        updateTimerLabel();
    }

    @FXML
    private void startButton() {
        if (running) return;

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            if (seconds == 60) {
                seconds = 0;
                minutes++;
            }
            if (minutes == 60) {
                minutes = 0;
                hours++;
            }
            updateTimerLabel();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        running = true;
    }

    @FXML
    private void stopButton() {
        if (timeline != null) timeline.stop();
        running = false;
        saveSession();
    }

    @FXML
    private void resetbutton() {
        stopButton();
        hours = minutes = seconds = 0;
        updateTimerLabel();
    }

    @FXML
    private void showPreviousRecords() {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(RECORD_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            content.append("No records found.");
        }

        // Create TextArea for better display
        TextArea textArea = new TextArea(content.toString());
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane contentPane = new GridPane();
        contentPane.setMaxWidth(Double.MAX_VALUE);
        contentPane.add(textArea, 0, 0);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Study Records");
        alert.setHeaderText("Previous Study Sessions:");
        alert.getDialogPane().setContent(contentPane);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private void saveSession() {
        if (hours == 0 && minutes == 0 && seconds == 0) return;

        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String record = timestamp + " - Studied for " + time;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RECORD_FILE, true))) {
            writer.write(record);
            writer.newLine();
        } catch (IOException e) {
            showAlert("Error", "Unable to save record.");
        }
    }

    private void updateTimerLabel() {
        if (timerLabel != null) {
            timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    public void back(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
        Parent root = loader.load();
        DashboardController controller = loader.getController();
        controller.setUsername(username);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root,1000,600);
        stage.setScene(scene);
        stage.show();
    }
    public void backtoDashboard(javafx.event.ActionEvent actionEvent) throws IOException {
        back(actionEvent);
    }

}