package com.example.project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import java.io.BufferedReader;
import java.io.FileReader;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class HelloController {

    @FXML
    private Label negga;
    @FXML
    private TextField userIdField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button Click;
    private Button Mail;
    private Button Call;
    @FXML
    private Button loginButton;
    @FXML
    public void login(ActionEvent event) throws IOException {
        String userId = userIdField.getText();
        String password = passwordField.getText();

        if (userId.isEmpty() || password.isEmpty()) {
            showAlert("Please enter both username and password.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] userDetails = line.split(",");
                if (userDetails[0].equals(userId) && userDetails[1].equals(password)) {
                    showAlert("Login successful!");
                    CurrentUser.username = userId;

                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
                    delay.setOnFinished(e -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
                            Parent root = loader.load();
                            DashboardController controller = loader.getController();
                            controller.setUsername(userId);
                            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                            Scene scene = new Scene(root, 1000, 600);
                            stage.setScene(scene);
                            stage.show();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            showAlert("Failed to load Dashboard.");
                        }
                    });
                    delay.play();

                    return;
                }
            }
            showAlert("Invalid username or password.");
        } catch (IOException e) {
            showAlert("Error reading user data.");
        }

    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }


    @FXML
    private void closeprgrm() {
        System.exit(0);
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("forgot_password.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root,1000,600);
        stage.setScene(scene);
        stage.show();
    }
    @FXML
    private void handleGetMail(ActionEvent event) throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource("mail.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root,1000,600));
        stage.show();

    }
    @FXML
    private void handleCall(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("call.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root,1000,600));
        stage.show();
    }
    @FXML
    private void openFacebook(ActionEvent event) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.google.com"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void click(javafx.event.ActionEvent actionEvent) throws IOException {
        login(actionEvent);
    }

    @FXML
    public void close(javafx.event.ActionEvent actionEvent) {
        closeprgrm();
    }

    @FXML
    public void forgotPassword(javafx.event.ActionEvent actionEvent) throws IOException {
        handleForgotPassword(actionEvent);
    }
    @FXML
    public void getMail(javafx.event.ActionEvent actionEvent) throws IOException {
        handleGetMail(actionEvent);

    }
    @FXML
    public void getFbId(javafx.event.ActionEvent actionEvent) throws IOException {
        openFacebook(actionEvent);
    }
    @FXML
    public void getCallNumber(javafx.event.ActionEvent actionEvent) throws IOException {
        handleCall(actionEvent);
    }
}
