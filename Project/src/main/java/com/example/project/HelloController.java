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

import java.io.IOException;

public class HelloController {

    @FXML
    private Label negga;

    @FXML
    private Button Click;
    private Button Mail;
    private Button Call;

    @FXML
    private void dhon() {
        negga.setText("Hello World");
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
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    @FXML
    private void handleGetMail(ActionEvent event) throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource("mail.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();

    }
    @FXML
    private void handleCall(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("call.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
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
    public void click(javafx.event.ActionEvent actionEvent) {
        dhon();
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
