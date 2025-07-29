package com.example.project;

import com.example.project.model.Message;
import com.example.project.model.Room;
import com.example.project.network.ChatClient;
import com.example.project.network.RoomServer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.example.project.components.MessageBubble;
import javafx.scene.control.ListCell;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.Optional;

public class ChatRoomController {
    @FXML private BorderPane rootPane;
    @FXML private Button backBtn;
    @FXML private Label roomNameLabel;
    @FXML private Label usersCountLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private ListView<Message> messagesListView;
    @FXML private TextField messageTextField;
    @FXML private Button sendBtn;
    @FXML private ListView<String> usersListView;


    @FXML private Button actionBtn; // Will be "Back" or "End Room"
    @FXML private VBox usersListContainer; // Container for user list with controls

    private Room currentRoom;
    private ChatClient chatClient;
    private String username;
    private ObservableList<Message> messages = FXCollections.observableArrayList();
    private ObservableList<String> onlineUsers = FXCollections.observableArrayList();


    private RoomServer hostedServer;
    private boolean isHost = false;

    public void setHostedServer(RoomServer server) {
        this.hostedServer = server;
    }

    public void setIsHost(boolean host) {
        this.isHost = host;
    }

    public boolean getIsHost() { return this.isHost;}

    public String getUsername() { return this.username; }

    @FXML
    public void initialize() {
        messagesListView.setItems(messages);
        usersListView.setItems(onlineUsers);
        usersListView.setCellFactory(listView -> new UserListCell());


        messagesListView.setCellFactory(listView -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);

                if (empty || message == null) {
                    setGraphic(null);
                } else {
                    MessageBubble bubble = new MessageBubble(message);
                    setGraphic(bubble);
                }
            }
        });

        // Auto-scroll to bottom when new messages arrive
        messages.addListener((javafx.collections.ListChangeListener<Message>) change -> {
            Platform.runLater(() -> {
                if (!messages.isEmpty()) {
                    messagesListView.scrollTo(messages.size() - 1);
                }
            });
        });

        // Focus on message input
        Platform.runLater(() -> messageTextField.requestFocus());
    }

    public void initializeRoom(Room room, String username, String password) {
        this.currentRoom = room;
        this.username = username;

        updateUIForHost();

        // Update UI
        roomNameLabel.setText(room.getRoomName());
        updateUsersCount(room.getCurrentUsers(), room.getMaxUsers());

        // Connect to room
        connectToRoom(password);
    }


    private void connectToRoom(String password) {
        connectionStatusLabel.setText("🟡 Connecting...");

        chatClient = new ChatClient(currentRoom, username, password);

        chatClient.setOnMessageReceived(this::onMessageReceived);
        chatClient.setOnUserListUpdated(this::onUserListUpdated);
        chatClient.setOnConnectionStatusChanged(this::onConnectionStatusChanged);
        chatClient.setOnError(this::onError);

        chatClient.setOnServerShutdown(this::onServerShutdown);
        chatClient.setOnKicked(this::onKicked);
        chatClient.setOnMuted(this::onMuted);

        // Connect in background thread
        Thread connectThread = new Thread(() -> {
            try {
                boolean connected = chatClient.connect();
                if (!connected) {
                    Platform.runLater(() -> {
                        connectionStatusLabel.setText("🔴 Connection Failed");
                        showAlert("Failed to connect to room", "Connection Error");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    connectionStatusLabel.setText("🔴 Connection Error");
                    showAlert("Connection error: " + e.getMessage(), "Error");
                    e.printStackTrace();
                });
            }
        });
        connectThread.setDaemon(true);
        connectThread.start();
    }


    private void updateUIForHost() {
        if (isHost) {
            actionBtn.setText("End Room");
            actionBtn.setStyle(actionBtn.getStyle() + "-fx-text-fill: #ff4444;");
        } else {
            actionBtn.setText("Back");
        }
    }

    @FXML
    public void onActionButtonClicked(ActionEvent event) throws IOException {
        if (isHost) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("End Room");
            confirmDialog.setHeaderText("Are you sure you want to end this room?");
            confirmDialog.setContentText("All participants will be disconnected.");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Stop the hosted server
                if (hostedServer != null) {
                    hostedServer.stopServer();
                }

                // Disconnect chat client
                if (chatClient != null) {
                    chatClient.disconnect();
                }

                // Navigate back to rooms
                goBack(event);
            }
        } else {
            goBack(event);
        }
    }



    private class UserListCell extends ListCell<String> {
        private HBox container;
        private Label userLabel;
        private Button kickBtn;
        private Button muteBtn;

        public UserListCell() {
            super();
            createControls();
        }

        private void createControls() {
            container = new HBox(10);
            container.setAlignment(Pos.CENTER_LEFT);

            userLabel = new Label();
            userLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            if (isHost) {
                kickBtn = new Button("Kick");
                kickBtn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-size: 10px;");
                kickBtn.setPrefWidth(50);

                muteBtn = new Button("Mute");
                muteBtn.setStyle("-fx-background-color: #ffaa00; -fx-text-fill: white; -fx-font-size: 10px;");
                muteBtn.setPrefWidth(50);

                container.getChildren().addAll(userLabel, kickBtn, muteBtn);

                kickBtn.setOnAction(e -> kickUser(getItem()));
                muteBtn.setOnAction(e -> muteUser(getItem()));
            } else {
                container.getChildren().add(userLabel);
            }
        }

        @Override
        protected void updateItem(String username, boolean empty) {
            super.updateItem(username, empty);

            if (empty || username == null) {
                setGraphic(null);
            } else {
                userLabel.setText(username);

                // Hide buttons for the host's own entry
                if (isHost && kickBtn != null && muteBtn != null) {
                    boolean isOwnEntry = username.equals(getUsername());
                    kickBtn.setVisible(!isOwnEntry);
                    muteBtn.setVisible(!isOwnEntry);

                    if (!isOwnEntry && hostedServer != null) {
                        boolean isMuted = hostedServer.isUserMuted(username);
                        muteBtn.setText(isMuted ? "Unmute" : "Mute");
                        muteBtn.setStyle(isMuted ?
                                "-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-size: 10px;" :
                                "-fx-background-color: #ffaa00; -fx-text-fill: white; -fx-font-size: 10px;");
                    }
                }

                setGraphic(container);
            }
        }
    }

    private void kickUser(String username) {
        if (hostedServer != null && username != null) {
            hostedServer.kickUser(username);
        }
    }

    private void muteUser(String username) {
        if (hostedServer != null && username != null) {
            if (hostedServer.isUserMuted(username)) {
                hostedServer.unmuteUser(username);
            } else {
                hostedServer.muteUser(username);
            }
            Platform.runLater(() -> usersListView.refresh());
        }
    }

    private void onMessageReceived(Message message) {
        Platform.runLater(() -> {
            messages.add(message);
        });
    }

    private void onUserListUpdated(java.util.List<String> users) {
        Platform.runLater(() -> {
            onlineUsers.clear();
            onlineUsers.addAll(users);
            updateUsersCount(users.size(), currentRoom.getMaxUsers());
        });
    }

    private void onConnectionStatusChanged(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                connectionStatusLabel.setText("🟢 Connected");
            } else {
                connectionStatusLabel.setText("🔴 Disconnected");
            }
        });
    }

    private void onError(String error) {
        Platform.runLater(() -> {
            showAlert(error, "Chat Error");
        });
    }

    private void onServerShutdown(String reason) {
        Platform.runLater(() -> {
            showAlert("Room has been closed: " + reason, "Room Closed");
            navigateBackToRooms();
        });
    }

    private void onKicked(String reason) {
        Platform.runLater(() -> {
            showAlert("You have been kicked: " + reason, "Kicked from Room");
            navigateBackToRooms();
        });
    }

    private void onMuted(Boolean isMuted) {
        Platform.runLater(() -> {
            if (isMuted) {
                messageTextField.setPromptText("You are muted and cannot send messages");
                messageTextField.setDisable(true);
                sendBtn.setDisable(true);
            } else {
                messageTextField.setPromptText("Type your message...");
                messageTextField.setDisable(false);
                sendBtn.setDisable(false);
            }
        });
    }

    private void navigateBackToRooms() {
        try {
            // Cleanup first
            if (chatClient != null) {
                chatClient.disconnect();
            }

            // Navigate back to Rooms
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Rooms.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate back to rooms: " + e.getMessage());
        }
    }

    private void updateUsersCount(int current, int max) {
        usersCountLabel.setText("👥 " + current + "/" + max);
    }

    @FXML
    public void sendMessage() {
        String messageText = messageTextField.getText().trim();
        if (messageText.isEmpty() || chatClient == null) {
            return;
        }

        chatClient.sendMessage(messageText);
        messageTextField.clear();
        messageTextField.requestFocus();
    }

    @FXML
    public void goBack(ActionEvent event) throws IOException {
        // Disconnect from chat
        if (chatClient != null) {
            chatClient.disconnect();
        }

        // Navigate back to Rooms
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Rooms.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void cleanup() {
        if (chatClient != null) {
            chatClient.disconnect();
        }
    }
}