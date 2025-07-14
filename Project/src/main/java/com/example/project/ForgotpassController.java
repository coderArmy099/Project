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
import java.util.EventObject;

public class ForgotpassController
{
    @FXML
   private  Button Back;
    private Button Exit;
    
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