package com.example.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import java.io.IOException;
import java.util.Optional;
public class DashboardController {
    @FXML
    public Button StudyTimer;
    @FXML
    private Button Home;
    @FXML
    private  Button Logout;
    @FXML
    private Label welcomeName;
    @FXML
    private Button hiveButton;


    private void setlogout(ActionEvent actionEvent) throws IOException {
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("Click Yes to confirm, or No to stay on the dashboard.");

        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No");

        alert.getButtonTypes().setAll(yes, no);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == yes) {

            setHome(actionEvent);
        }
    }

    private void setStudyTimer(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("studyTimer.fxml"));
        Parent root = loader.load();

        // Send username
        StudyTimerController controller = loader.getController();
        controller.setUsername(welcomeName.getText().replace("Welcome ", ""));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }


    private void setSchedule(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Calendar.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.show();
    }

    private void setRooms(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Rooms.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.show();
    }

    private void setHome(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        Parent root = loader.load();


        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root,1000,600);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void schedule(javafx.event.ActionEvent actionEvent) throws IOException {
        setSchedule(actionEvent);
    }

    @FXML
    public void Rooms(javafx.event.ActionEvent actionEvent) throws IOException {
        setRooms(actionEvent);
    }

    @FXML
    public void studyTimer(javafx.event.ActionEvent actionEvent) throws IOException {
        setStudyTimer(actionEvent);
    }
    @FXML
    public void goToHome(javafx.event.ActionEvent actionEvent) throws IOException {
        setHome(actionEvent);
    }
    @FXML
    public void logout(javafx.event.ActionEvent actionEvent)throws IOException
    {
        setlogout(actionEvent);
    }
    public void setUsername(String name) {
        welcomeName.setText(name);
    }
}
