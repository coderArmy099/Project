package com.example.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.example.project.model.Room;
import com.example.project.network.RoomServer;
import com.example.project.network.RoomDiscovery;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import java.util.HashMap;
import java.util.Map;
import java.net.ServerSocket;
import com.example.project.CurrentUser;

import java.io.IOException;

public class RoomsController {


    private RoomServer hostedRoomServer;
    private RoomDiscovery roomDiscovery;
    private Map<String, Room> discoveredRooms = new HashMap<>();

    @FXML private BorderPane rootPane;
    @FXML private StackPane rootStack;
    @FXML private VBox navPane;
    @FXML private Label hivesLabel;
//    @FXML private ListView<String> roomsListView;
    @FXML private ScrollPane roomsScrollPane;
    @FXML private GridPane roomsGridPane;
    @FXML private Button calendarBtn;
    @FXML private Button timerBtn;
    @FXML private Button communityBtn;

    // Create Hive form elements
    @FXML private Button createHiveBtn;
    @FXML private VBox createHivePane;
    @FXML private GridPane formGrid;
    @FXML private TextField roomNameField;
    @FXML private PasswordField passwordField;
    @FXML private ChoiceBox<String> accessChoice;
    @FXML private Spinner<Integer> durationSpin;
    @FXML private Button cancelBtn;
    @FXML private Button createBtn;

    @FXML
    public void initialize() {
        // Initialize access choice box
        accessChoice.getItems().addAll("Public", "Private");
        accessChoice.setValue("Public");

        // Initialize duration spinner
        durationSpin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 480, 60, 15));

        // Initially disable password field for public rooms
        passwordField.setDisable(true);

        createHivePane.prefHeightProperty().bind(roomsScrollPane.heightProperty().multiply(0.8));
        createHivePane.maxHeightProperty().bind(roomsScrollPane.heightProperty().multiply(0.5));
        createHivePane.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.5));
        createHivePane.maxWidthProperty().bind(rootPane.widthProperty().multiply(0.5));

        // Handling room discovery
        startRoomDiscovery();
    }




    @FXML
    public void hideCreateHiveForm() {
        createHivePane.setVisible(false);
        createHivePane.setManaged(false);

        // Clear form
        roomNameField.clear();
        passwordField.clear();
        accessChoice.setValue("Public");
        durationSpin.getValueFactory().setValue(60);
        durationSpin.editableProperty().setValue(true);
    }

    @FXML
    public void showCreateHiveForm() {
        if (createHivePane.isVisible()) {
            hideCreateHiveForm();
        } else {
            createHivePane.setVisible(true);
            createHivePane.setManaged(true);
        }
    }




    @FXML
    public void onAccessChanged() {
        String selectedAccess = accessChoice.getValue();
        if ("Private".equals(selectedAccess)) {
            passwordField.setDisable(false);
        } else {
            passwordField.setDisable(true);
            passwordField.clear();
        }
    }


    private int findAvailablePort() {
        for (int port = 9000; port <= 9999; port++) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                // Port is not available, try next one
            }
        }
        throw new RuntimeException("No available ports found");
    }

    private String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1"; // Fallback to localhost
        }
    }

    @FXML
    public void createHive() {
        String roomName = roomNameField.getText().trim();
        String access = accessChoice.getValue();
        String password = passwordField.getText();
        int duration = durationSpin.getValue();

        if (roomName.isEmpty()) {
            showAlert("Please enter a room name", "Input Error");
            return;
        }

        if ("Private".equals(access) && password.isEmpty()) {
            showAlert("Please set a password for private room", "Input Error");
            return;
        }

        try {
            // Find an available port
            int port = findAvailablePort();
            String localIP = RoomDiscovery.getLocalIPAddress();

            // Create the room
            Room newRoom = new Room(roomName, localIP, port, "Private".equals(access), password, duration);

            // Start the server
            hostedRoomServer = new RoomServer(newRoom);

            // Start server in background thread
            Thread serverThread = new Thread(() -> hostedRoomServer.startServer());
            serverThread.setDaemon(true);
            serverThread.start();

            System.out.println("Room created successfully: " + roomName + " on port " + port);

            // TODO: Navigate to chat room as host
             showAlert("Room created successfully!\nRoom: " + roomName + "\nPort: " + port);

            hideCreateHiveForm();

        } catch (Exception e) {
            showAlert("Failed to create room: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String message) {
        showAlert(message, "Error");
    }


    // Room handling part(discovery, display, etc.)
    private void startRoomDiscovery() {
        roomDiscovery = new RoomDiscovery();

        roomDiscovery.startListening(room -> {
            // this lambda implements accept method of Consumer interface
            // This runs on the background thread, Platform.runLater schedules UI chnages on the javafx application thread
            Platform.runLater(() -> {
                // this implements runnable interface, the lambda expression is used to provide the implementation of run method
                String roomKey = room.getRoomName() + "->" + room.getHostIP() + ":" + room.getPort();

//                // Add or update the room
//                discoveredRooms.put(roomKey, room);
//
//                // Refresh the display
//                displayDiscoveredRooms();

                // only add new keys
                if (!discoveredRooms.containsKey(roomKey)) {
                    discoveredRooms.put(roomKey, room);
                    displayDiscoveredRooms();
                }
            });
        });
    }

    private void displayDiscoveredRooms() {
        roomsGridPane.getChildren().clear();

        int row = 0;
        int col = 0;

        for (Room room : discoveredRooms.values()) {
            Button roomButton = createRoomButton(room);

            roomsGridPane.add(roomButton, col, row);

            col++;
            if (col >= 2) { // 2 columns
                col = 0;
                row++;
            }
        }
    }

    private Button createRoomButton(Room room) {
        Button roomButton = new Button();
        roomButton.setPrefWidth(400);
        roomButton.setPrefHeight(100);
        roomButton.setMaxWidth(Double.MAX_VALUE);

        // Create room info layout
        VBox roomInfo = new VBox(10);
        roomInfo.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(room.getRoomName());
        nameLabel.setStyle(
                "-fx-font-size: 22px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #00BFA5;" +
                        "-fx-font-family: 'Artifakt Element Heavy'"
        );

        HBox detailsBox = new HBox(15);
        detailsBox.setAlignment(Pos.CENTER_LEFT);

        Label usersLabel = new Label("👥 " + room.getCurrentUsers() + "/" + room.getMaxUsers());
        Label durationLabel = new Label("⏱️" + room.getDuration() + " min");
        Label statusLabel = new Label(room.isPrivate() ? "🔒 Private" : "🌐 Public");

        // White/off-white text for details
        String detailStyle = "-fx-font-size: 13px; -fx-text-fill: #E8E8E8; -fx-font-weight: 500;";
        usersLabel.setStyle(detailStyle);
        durationLabel.setStyle(detailStyle);
        statusLabel.setStyle(detailStyle);

        detailsBox.getChildren().addAll(usersLabel, durationLabel, statusLabel);
        roomInfo.getChildren().addAll(nameLabel, detailsBox);

        roomButton.setGraphic(roomInfo);

        // Dark gray background with teal accent and rounded corners
        roomButton.setStyle(
                "-fx-background-color: #222222; " +
                        "-fx-border-color: #00BFA5; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 15px; " +
                        "-fx-background-radius: 15px; " +
                        "-fx-padding: 20px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 191, 165, 0.3), 8, 0, 0, 2);"
        );

        // Enhanced hover effect with teal glow
        roomButton.setOnMouseEntered(e -> roomButton.setStyle(
                "-fx-background-color: #3A3A3A; " +
                        "-fx-border-color: #00E5CC; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 15px; " +
                        "-fx-background-radius: 15px; " +
                        "-fx-padding: 20px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 191, 165, 0.6), 12, 0, 0, 3);"
        ));

        roomButton.setOnMouseExited(e -> roomButton.setStyle(
                "-fx-background-color: #222222; " +
                        "-fx-border-color: #00BFA5; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 15px; " +
                        "-fx-background-radius: 15px; " +
                        "-fx-padding: 20px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 191, 165, 0.3), 8, 0, 0, 2);"
        ));

        // Handle room joining
        roomButton.setOnAction(e -> joinRoom(room));

        return roomButton;
    }




    private void joinRoom(Room room) {
        // Check if room is full
        if (room.getCurrentUsers() >= room.getMaxUsers()) {
            showAlert("Room is full!");
            return;
        }

        String password = "";
        // Handle private room password
        if (room.isPrivate()) {
            String enteredPassword = promptForPassword();
            if (enteredPassword == null) return; // User cancelled
            password = enteredPassword;
        }

        // Get username
        String username = CurrentUser.getUsername();
        if (username == null || username.trim().isEmpty()) return;

        try {
            // Navigate to chat room
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatRoom.fxml"));
            Parent root = loader.load();

            ChatRoomController chatController = loader.getController();
            chatController.initializeRoom(room, username.trim(), password);
            chatController.setHostedServer(hostedRoomServer);  // hand off the RoomServer
            chatController.setIsHost(true);

            Stage stage = (Stage) createHiveBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

//            // Cleanup current controller
//            //cleanup();
            if (roomDiscovery != null) {
                roomDiscovery.cleanup();
            }

        } catch (IOException e) {
            showAlert("Failed to open chat room: " + e.getMessage());
            e.printStackTrace();
        }
    }

//    // Add this helper method
//    private String promptForUsername() {
//        TextInputDialog dialog = new TextInputDialog();
//        dialog.setTitle("Join Room");
//        dialog.setHeaderText("Enter your username");
//        dialog.setContentText("Username:");
//
//        return dialog.showAndWait().orElse(null);
//    }

    // NEW: Password input dialog
    private String promptForPassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Private Room");
        dialog.setHeaderText("This room requires a password");
        dialog.setContentText("Please enter the password:");

        return dialog.showAndWait().orElse(null);
    }

    public void cleanup() {
        if (roomDiscovery != null) {
            roomDiscovery.cleanup();
        }
    }



    // Navigation to other fxmls
    private void setCalendar(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Calendar.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void goToCalendar(ActionEvent actionEvent) throws IOException {
        cleanup();
        setCalendar(actionEvent);
    }
}