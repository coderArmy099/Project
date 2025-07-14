package com.example.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class mailcontroller {
    private  void setBack(ActionEvent event)throws IOException
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        Parent root = loader.load();


        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    private void closeprgrm() {
        System.exit(0);
    }

    public  void backtomain(javafx.event.ActionEvent actionEvent)throws IOException
    {
        setBack(actionEvent);
    }
    public void close(javafx.event.ActionEvent actionEvent) {
        closeprgrm();
    }
}
