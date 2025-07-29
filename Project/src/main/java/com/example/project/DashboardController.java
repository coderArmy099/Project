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


    public void initialize() {
        if (dashboardBtn != null) dashboardBtn.setOnAction(this::goToDashboard);
        if (courseTrackerNavBtn != null) courseTrackerNavBtn.setOnAction(this::goToCourseTracker);
        if (calendarBtn != null) calendarBtn.setOnAction(this::goToCalendar);
        if (timerBtn != null) timerBtn.setOnAction(this::goToStudyTimer);
        if (communityBtn != null) communityBtn.setOnAction(this::goToCommunity);
        // Setup logout button and user info
        if (logoutBtn != null) logoutBtn.setOnAction(this::handleLogout);
        loadUserInfo();
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
