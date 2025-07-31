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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.OverrunStyle;

import com.example.project.components.MessageBubble;
import com.example.project.network.FileTransferClient;
import javafx.scene.control.ListCell;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.layout.Region;

import java.awt.*;
import java.io.File;
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
    @FXML private Button fileUpBtn;
    @FXML private ListView<String> usersListView;
    @FXML private Label sessionTimerLabel;

    @FXML private Button actionBtn; // Will be "Back" or "End Room"
    @FXML private VBox usersListContainer; // Container for user list with controls

    private Room currentRoom;
    private ChatClient chatClient;
    private String username;
    private ObservableList<Message> messages = FXCollections.observableArrayList();
    private ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private Timeline sessionTimer;
    private int sessionTimeRemaining;


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
                    MessageBubble bubble = new MessageBubble(message, messagesListView);
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

        initializeSessionTimer();

        Platform.runLater(() -> messageTextField.requestFocus());
    }

    private void initializeSessionTimer() {
        sessionTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateSessionTimer()));
        sessionTimer.setCycleCount(Timeline.INDEFINITE);
        sessionTimer.play();
        updateSessionTimerDisplay();
    }

    private void updateSessionTimer() {
        sessionTimeRemaining--;
        updateSessionTimerDisplay();

        if (sessionTimeRemaining <= 0) {
            sessionTimer.stop();
            // Handle session timeout
            Platform.runLater(() -> {
                showAlert("Session has expired", "Session Timeout");
                navigateBackToRooms();
            });
        }
    }

    private void updateSessionTimerDisplay() {
        int hours = sessionTimeRemaining / 3600;
        int remainingAfterHours = sessionTimeRemaining % 3600;
        int minutes = remainingAfterHours / 60;
        int seconds = remainingAfterHours % 60;
        Platform.runLater(() -> {
            sessionTimerLabel.setText(String.format("%02d:%02d:%02d",hours, minutes, seconds));
        });
    }

    public void initializeRoom(Room room, String username, String password) {
        this.currentRoom = room;
        this.username = username;


        this.sessionTimeRemaining = room.getDuration() * 60;

        updateUIForHost();

        // Update UI
        roomNameLabel.setText(room.getRoomName());
        updateUsersCount(room.getCurrentUsers(), room.getMaxUsers());

        // Connect to room
        connectToRoom(password);
    }


    private void connectToRoom(String password) {
        connectionStatusLabel.setText("Connecting...");

        chatClient = new ChatClient(currentRoom, username, password);

        chatClient.setOnMessageReceived(this::onMessageReceived);
        chatClient.setOnUserListUpdated(this::onUserListUpdated);
        chatClient.setOnConnectionStatusChanged(this::onConnectionStatusChanged);
        chatClient.setOnError(this::onError);
        chatClient.setOnServerShutdown(this::onServerShutdown);
        chatClient.setOnKicked(this::onKicked);
        chatClient.setOnMuted(this::onMuted);
        chatClient.setOnRemainingTimeReceived(this::onRemainingTimeReceived);

        Thread connectThread = new Thread(() -> {
            boolean connectionResult = chatClient.connect();
            Platform.runLater(() -> {
                if (connectionResult) {
                    connectionStatusLabel.setText("Connected");

                    // Initialize file transfer with the server's file port
//                    if (hostedServer != null) {
//                        chatClient.initializeFileTransfer(hostedServer.getFilePort());
//                    } else {
//                        // For clients, you might need to get file port from server
//                        chatClient.initializeFileTransfer(currentRoom.getPort() + 1000); // Temporary solution
//                    }

                    // Set reference for MessageBubble
                    MessageBubble.setChatClientReference(chatClient);
                } else {
                    connectionStatusLabel.setText("Failed to connect");
                }
            });
        });
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void updateUIForHost() {
        if (isHost) {
            FontIcon endRoomIcon = new FontIcon("fas-door-closed");
            actionBtn.setGraphic(endRoomIcon);
        } else {
            FontIcon backIcon = new FontIcon("fas-arrow-left");
            actionBtn.setGraphic(backIcon);
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

    @FXML
    public void onFileUploadButtonClicked(ActionEvent event) {
        if (chatClient == null || !chatClient.isConnected()) {
            showAlert("You must be connected to upload files.", "Connection Error");
            return;
        }

        // Check if the user is muted
        if (hostedServer != null && hostedServer.isUserMuted(username)) {
            showAlert("You are muted and cannot upload files.", "Muted User");
            return;
        }
        handleFileUpload();
    }



    private void handleFileUpload() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select File to Share");

        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*"),
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.gif"),
                new javafx.stage.FileChooser.ExtensionFilter("Documents", "*.txt", "*.pdf", "*.doc")
        );

        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedFile != null) {
            long maxSize = 10 * 1024 * 1024;
            if (selectedFile.length() > maxSize) {
                showAlert("File too large. Maximum size is 10MB.", "File Size Error");
                return;
            }

            Alert uploadDialog = new Alert(Alert.AlertType.NONE);
            uploadDialog.setTitle("Uploading File");
            uploadDialog.setHeaderText("Uploading: " + selectedFile.getName());
            uploadDialog.setContentText("Please wait...");
            // Use a ButtonType to allow closing the dialog programmatically
            uploadDialog.getButtonTypes().add(ButtonType.CANCEL);
            uploadDialog.show();

            // Upload in background thread
            Thread uploadThread = new Thread(() -> {
                boolean success = chatClient.uploadFile(selectedFile);
                Platform.runLater(() -> {
                    uploadDialog.close(); // Close the dialog
                    if (success) {
                        // The server broadcast will add the message, no need for another alert.
                    } else {
                        showAlert("Failed to upload file.", "Upload Error");
                    }
                });
            });
            uploadThread.setDaemon(true);
            uploadThread.start();
        }
    }



    private class UserListCell extends ListCell<String> {
        private HBox container;
        private Label userLabel;
        private Button kickBtn;
        private Button muteBtn;

        // Fixed dimensions
        private static final double CONTAINER_WIDTH = 245;
        private static final double CONTAINER_HEIGHT = 32;
        private static final double HORIZONTAL_PADDING = 8;
        private static final double BUTTON_SPACING = 6;
        private static final double KICK_BTN_WIDTH = 45;
        private static final double MUTE_BTN_MIN_WIDTH = 45;
        private static final double MUTE_BTN_MAX_WIDTH = 60;

        public UserListCell() {
            super();
            createControls();
        }

        private void createControls() {
            container = new HBox(BUTTON_SPACING);
            container.setAlignment(Pos.CENTER_LEFT);

            // Fixed container dimensions
            container.setPrefWidth(CONTAINER_WIDTH);
            container.setMaxWidth(CONTAINER_WIDTH);
            container.setMinWidth(CONTAINER_WIDTH);
            container.setPrefHeight(CONTAINER_HEIGHT);
            container.setMaxHeight(CONTAINER_HEIGHT);
            container.setMinHeight(CONTAINER_HEIGHT);

            container.setStyle(
                    "-fx-background-color: #1a1a1a; " +
                            "-fx-background-radius: 12px; " +
                            "-fx-padding: " + HORIZONTAL_PADDING + " " + HORIZONTAL_PADDING + " " + HORIZONTAL_PADDING + " " + HORIZONTAL_PADDING + "; " +
                            "-fx-border-color: #333333; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 12px;"
            );

            userLabel = new Label();
            userLabel.setStyle(
                    "-fx-text-fill: white; " +
                            "-fx-font-size: 11px; " +
                            "-fx-font-family: 'Artifakt Element';"
            );

            // Calculate available width for username
            double availableWidth = CONTAINER_WIDTH - (2 * HORIZONTAL_PADDING);

            if (isHost) {
                // Reserve space for buttons
                double buttonSpace = KICK_BTN_WIDTH + MUTE_BTN_MAX_WIDTH + (2 * BUTTON_SPACING);
                double userLabelWidth = availableWidth - buttonSpace;

                userLabel.setPrefWidth(userLabelWidth);
                userLabel.setMaxWidth(userLabelWidth);
                userLabel.setMinWidth(0);

                kickBtn = new Button("Kick");
                kickBtn.setPrefWidth(KICK_BTN_WIDTH);
                kickBtn.setMaxWidth(KICK_BTN_WIDTH);
                kickBtn.setMinWidth(KICK_BTN_WIDTH);
                kickBtn.setPrefHeight(22);
                kickBtn.setMaxHeight(22);
                kickBtn.setStyle(
                        "-fx-background-color: #ff4444; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 10px; " +
                                "-fx-font-family: 'Artifakt Element'; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-border-radius: 8px; " +
                                "-fx-padding: 2 4;"
                );

                muteBtn = new Button("Mute");
                muteBtn.setMinWidth(MUTE_BTN_MIN_WIDTH);
                muteBtn.setMaxWidth(MUTE_BTN_MAX_WIDTH);
                muteBtn.setPrefHeight(22);
                muteBtn.setMaxHeight(22);
                muteBtn.setStyle(
                        "-fx-background-color: #ffaa00; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 10px; " +
                                "-fx-font-family: 'Artifakt Element'; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-border-radius: 8px; " +
                                "-fx-padding: 2 4;"
                );

                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                container.getChildren().addAll(userLabel, spacer, muteBtn, kickBtn);

                kickBtn.setOnAction(e -> kickUser(getItem()));
                muteBtn.setOnAction(e -> muteUser(getItem()));
            } else {
                // No buttons, username can use full width
                userLabel.setPrefWidth(availableWidth);
                userLabel.setMaxWidth(availableWidth);
                userLabel.setMinWidth(0);
                container.getChildren().add(userLabel);
            }

            // Enable text truncation for long usernames
            userLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        }

        @Override
        protected void updateItem(String username, boolean empty) {
            super.updateItem(username, empty);

            if (empty || username == null) {
                setGraphic(null);
            } else {
                // Check if this user is the host and add indicator
                boolean isHostUser = isHost && username.equals(getUsername());
                String displayName = isHostUser ? username + " - Host" : username;

                if (isHostUser) {
                    userLabel.setStyle(
                            "-fx-text-fill: #00BFA5; " +
                                    "-fx-font-size: 11px; " +
                                    "-fx-font-family: 'Artifakt Element'; " +
                                    "-fx-font-weight: bold;"
                    );
                } else {
                    userLabel.setStyle(
                            "-fx-text-fill: white; " +
                                    "-fx-font-size: 11px; " +
                                    "-fx-font-family: 'Artifakt Element';"
                    );
                }

                userLabel.setText(displayName);

                // Hide buttons for the host's own entry
                if (isHost && kickBtn != null && muteBtn != null) {
                    boolean isOwnEntry = username.equals(getUsername());
                    kickBtn.setVisible(!isOwnEntry);
                    muteBtn.setVisible(!isOwnEntry);

                    if (!isOwnEntry && hostedServer != null) {
                        boolean isMuted = hostedServer.isUserMuted(username);
                        String buttonText = isMuted ? "Unmute" : "Mute";
                        muteBtn.setText(buttonText);

                        // Adjust button width based on text
                        double textWidth = buttonText.equals("Unmute") ? MUTE_BTN_MAX_WIDTH : MUTE_BTN_MIN_WIDTH;
                        muteBtn.setPrefWidth(textWidth);

                        muteBtn.setStyle(isMuted ?
                                "-fx-background-color: #4caf50; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 10px; " +
                                        "-fx-font-family: 'Artifakt Element'; " +
                                        "-fx-background-radius: 8px; " +
                                        "-fx-border-radius: 8px; " +
                                        "-fx-padding: 2 4;" :
                                "-fx-background-color: #ffaa00; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 10px; " +
                                        "-fx-font-family: 'Artifakt Element'; " +
                                        "-fx-background-radius: 8px; " +
                                        "-fx-border-radius: 8px; " +
                                        "-fx-padding: 2 4;"
                        );
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

            if (message.isFileMessage()) {
                // You can add a download button to the message bubble
                // or handle it in the MessageBubble component
            }
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

    private void onRemainingTimeReceived(Integer remainingSeconds) {
        Platform.runLater(() -> {
            if (!isHost) { // Only update for clients
                this.sessionTimeRemaining = remainingSeconds;
                updateSessionTimerDisplay();
            }
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
            Scene scene = new Scene(root, 1280, 720);
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
        Scene scene = new Scene(root, 1280, 720);
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
        if (sessionTimer != null) {
            sessionTimer.stop();
        }
        if (chatClient != null) {
            chatClient.disconnect();
        }
    }
}