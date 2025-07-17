package com.example.project;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.FileWriter;
import java.io.IOException;

public class SignupController {

    @FXML
    private TextField userIdField, schoolField, nameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signupButton;

    @FXML
    public void signup() {
        String userId = userIdField.getText();
        String password = passwordField.getText();
        String school = schoolField.getText();
        String name = nameField.getText();

        if (userId.isEmpty() || password.isEmpty() || school.isEmpty() || name.isEmpty()) {
            showAlert("All fields must be filled.");
            return;
        }

        try (FileWriter writer = new FileWriter("users.txt", true)) {
            writer.write(userId + "," + password + "," + school + "," + name + "\n");
            showAlert("Sign-up successful!");
        } catch (IOException e) {
            showAlert("Error saving user information.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }
}