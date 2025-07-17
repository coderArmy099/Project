package com.example.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {
    @FXML
    private Button Signup;
    @FXML
    private Button Login;

    private void closeprgrm() {
        System.exit(0);
    }
    private void signup(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("signup.fxml"));
        Parent root = loader.load();


        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root,1000,600);
        stage.setScene(scene);
        stage.show();
    }
    private void login(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        Parent root = loader.load();


        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root,1000,600);
        stage.setScene(scene);
        stage.show();
    }

    public void close(javafx.event.ActionEvent actionEvent) {
        closeprgrm();
    }


    public void openSignUp(javafx.event.ActionEvent actionEvent) throws IOException {
        signup(actionEvent);
    }
    public void openLogin(javafx.event.ActionEvent actionEvent) throws IOException {
        login(actionEvent);
    }

}
