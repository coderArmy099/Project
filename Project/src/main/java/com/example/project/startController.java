package com.example.project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.*;
import java.util.Objects;

public class startController {

    // UI Components
    @FXML private HBox loginPane;
    @FXML private HBox signupPane;

    // Login fields
    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button loginButton;

    // Signup fields
    @FXML private TextField signupNameField;
    @FXML private TextField signupSchoolField;
    @FXML private TextField signupUsernameField;
    @FXML private PasswordField signupPasswordField;
    @FXML private Button signupButton;

    @FXML
    public void initialize() {
        setupTransitions();

        // Show login pane by default
        loginPane.setVisible(true);
        signupPane.setVisible(false);
    }

    private void setupTransitions() {
        loginPane.setOpacity(1.0);
        signupPane.setOpacity(0.0);
    }

    @FXML
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill in all fields", Alert.AlertType.ERROR);
            return;
        }

        if (authenticateUser(username, password)) {
            showAlert("Success", "Login successful!", Alert.AlertType.INFORMATION);
            navigateToDashboard(username);
        } else {
            showAlert("Error", "Invalid username or password", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSignup() {
        String username = signupNameField.getText().trim(); // Use name field as username
        String school = signupSchoolField.getText().trim();
        String password = signupPasswordField.getText();

        if (username.isEmpty() || school.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill in all fields", Alert.AlertType.ERROR);
            return;
        }

        if (userExists(username)) {
            showAlert("Error", "Username already exists", Alert.AlertType.ERROR);
            return;
        }

        // Register user with username used for both username and userId
        if (registerUser(username, password, school, username)) { // Pass username twice
            showAlert("Success", "Account created successfully!", Alert.AlertType.INFORMATION);
            clearSignupFields();
            showLoginPane();
        } else {
            showAlert("Error", "Failed to create account", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void showSignupPane() {
        fadeTransition(loginPane, signupPane);
    }

    @FXML
    private void showLoginPane() {
        fadeTransition(signupPane, loginPane);
    }

    private void fadeTransition(HBox hidePane, HBox showPane) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), hidePane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), showPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        fadeOut.setOnFinished(e -> {
            hidePane.setVisible(false);
            showPane.setVisible(true);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private boolean authenticateUser(String username, String password) {
        String basePath = "Project/data/";
        new File(basePath).mkdirs();

        try (BufferedReader reader = new BufferedReader(new FileReader(basePath + "users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] userDetails = line.split(",");
                if (userDetails.length >= 2 && userDetails[0].equals(username) && userDetails[1].equals(password)) {
                    CurrentUser.setSchool( userDetails[2] );
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean registerUser(String username, String password, String school, String name) {
        String basePath = "Project/data/";
        new File(basePath).mkdirs();

        try (FileWriter writer = new FileWriter(basePath + "users.txt", true)) {
            writer.write(username + "," + password + "," + school + "," + name + "\n");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean userExists(String username) {
        String basePath = "Project/data/";
        try (BufferedReader reader = new BufferedReader(new FileReader(basePath + "users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] userDetails = line.split(",");
                if (userDetails.length > 0 && userDetails[0].equals(username)) {
                    return true;
                }
            }
        } catch (IOException e) {
            // File doesn't exist yet, which is fine
        }
        return false;
    }

    private void navigateToDashboard(String username) {
        try {
            //CurrentUser.username = username;
            CurrentUser.setUsername(username);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Dashboard", Alert.AlertType.ERROR);
        }
    }

    private void clearSignupFields() {
        signupNameField.clear();
        signupSchoolField.clear();
        signupPasswordField.clear();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}