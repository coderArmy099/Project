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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import com.example.project.components.MessageBubble;
import javafx.scene.control.ListCell;

import java.io.IOException;

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

    @FXML
    public void initialize() {
        messagesListView.setItems(messages);
        usersListView.setItems(onlineUsers);


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

        // Update UI
        roomNameLabel.setText(room.getRoomName());
        updateUsersCount(room.getCurrentUsers(), room.getMaxUsers());

        // Connect to room
        connectToRoom(password);
    }

    private void connectToRoom(String password) {
        connectionStatusLabel.setText("🟡 Connecting...");

        chatClient = new ChatClient(currentRoom, username, password);

        // Set up callbacks
        chatClient.setOnMessageReceived(this::onMessageReceived);
        chatClient.setOnUserListUpdated(this::onUserListUpdated);
        chatClient.setOnConnectionStatusChanged(this::onConnectionStatusChanged);
        chatClient.setOnError(this::onError);

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

    private void onMessageReceived(Message message) {
//        Platform.runLater(() -> {
//            String displayMessage;
//            if (message.isSystemMessage()) {
//                displayMessage = String.format("[%s] %s",
//                        message.getFormattedTime(), message.getContent());
//            } else {
//                displayMessage = String.format("[%s] %s: %s",
//                        message.getFormattedTime(), message.getUsername(), message.getContent());
//            }
//            messages.add(displayMessage);
//        });

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