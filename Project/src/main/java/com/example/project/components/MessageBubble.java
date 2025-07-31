package com.example.project.components;

import com.example.project.model.Message;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import com.example.project.CurrentUser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import java.io.File;
import com.example.project.network.ChatClient;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert;

public class MessageBubble extends HBox {
    private static final String CURRENT_USER = CurrentUser.getUsername();
    private ListView<?> parentListView;
    private static ChatClient chatClientReference;


    public MessageBubble(Message message, ListView<?> parentListView) {
        super();
        this.parentListView = parentListView;

        if (message.isSystemMessage()) {
            createSystemMessage(message);
        } else if (message.isFileMessage()) {
            createFileMessage(message);
        } else {
            boolean isCurrentUser = message.getUsername().equals(CURRENT_USER);
            createUserMessage(message, isCurrentUser);
        }
    }

    public MessageBubble(Message message) {
        this(message, null);
    }

    public static void setChatClientReference(ChatClient client) {
        chatClientReference = client;
    }




    private void downloadFile(String fileId, String fileName) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(fileName);

        java.io.File saveLocation = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (saveLocation != null && chatClientReference != null) {
            // Show download progress
            Alert downloadDialog = new Alert(Alert.AlertType.INFORMATION);
            downloadDialog.setTitle("Downloading File");
            downloadDialog.setHeaderText("Downloading: " + fileName);
            downloadDialog.setContentText("Please wait...");
            downloadDialog.show();

            Thread downloadThread = new Thread(() -> {
                boolean success = chatClientReference.downloadFile(fileId, saveLocation);

                Platform.runLater(() -> {
                    downloadDialog.close();
                    if (success) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Download Complete");
                        alert.setHeaderText(null);
                        alert.setContentText("File saved successfully!");
                        alert.showAndWait();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("error");
                        alert.setHeaderText(null);
                        alert.setContentText("error");
                        alert.showAndWait();
                    }
                });
            });
            downloadThread.setDaemon(true);
            downloadThread.start();
        }
    }

    private void createSystemMessage(Message message) {
        this.setAlignment(Pos.CENTER);
        this.setPrefWidth(Region.USE_COMPUTED_SIZE);
        this.setMaxWidth(Region.USE_COMPUTED_SIZE);

        Label systemLabel = new Label(message.getContent());
        systemLabel.setStyle(
                "-fx-text-fill: #888888; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-style: italic; " +
                        "-fx-padding: 5 10; " +
                        "-fx-background-color: #2a2a2a; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #444444; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-width: 1;"
        );

        this.getChildren().add(systemLabel);
    }

    private void createFileMessage(Message message) {
        boolean isCurrentUser = message.getUsername().equals(CURRENT_USER);
        this.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox container = new VBox(5);
        container.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Meta information
        String metaText = message.getUsername() + " • " + message.getFormattedTime();
        Label metaLabel = new Label(metaText);
        metaLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px; -fx-padding: 0 8 2 8;");

        // File message container
        HBox fileContainer = new HBox(10);
        fileContainer.setAlignment(Pos.CENTER_LEFT);
        fileContainer.setStyle(
                "-fx-background-color: " + (isCurrentUser ? "#00BFA5" : "#3a3a3a") + "; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );

        // File icon
        Label fileIcon = new Label("📎");
        fileIcon.setStyle("-fx-font-size: 20px;");

        // File info
        VBox fileInfo = new VBox(2);
        Label fileName = new Label("📄 " + message.getContent());
        fileName.setStyle(
                "-fx-text-fill: " + (isCurrentUser ? "white" : "#e0e0e0") + "; " +
                        "-fx-font-size: 14px; -fx-font-weight: bold;"
        );

        Label fileDescription = new Label("Shared file");
        fileDescription.setStyle(
                "-fx-text-fill: " + (isCurrentUser ? "#cccccc" : "#aaaaaa") + "; " +
                        "-fx-font-size: 12px;"
        );

        fileInfo.getChildren().addAll(fileName, fileDescription);

        // Download button (only for files from others)
        // Download button (only for files from others)
        Button downloadBtn = new Button();
        FontIcon downloadIcon = new FontIcon(FontAwesomeSolid.DOWNLOAD);
        downloadIcon.setIconSize(16);
        downloadIcon.setIconColor(Color.valueOf(isCurrentUser ? "white" : "#e0e0e0"));
        downloadBtn.setGraphic(downloadIcon);

        downloadBtn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 5;"
        );
        downloadBtn.setOnAction(e -> downloadFile(message.getFileId(), message.getContent()));

        fileContainer.getChildren().addAll(fileIcon, fileInfo, downloadBtn);
        container.getChildren().addAll(metaLabel, fileContainer);

        if (isCurrentUser) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            this.getChildren().addAll(spacer, container);
        } else {
            StackPane avatar = createAvatar(message.getUsername());
            this.getChildren().addAll(avatar, container);
        }
    }

    private void createUserMessage(Message message, boolean isCurrentUser) {
        this.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        this.setPrefWidth(Region.USE_COMPUTED_SIZE);
        this.setMaxWidth(Region.USE_COMPUTED_SIZE);

        if (isCurrentUser) {
            // Current user: no avatar, just meta info + bubble with extra padding (right aligned)
            VBox metaAndBubble = createMessageContainer(message, true);
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            // Add extra right padding to avoid scrollbar overlap
            HBox.setMargin(metaAndBubble, new Insets(0, 12, 0, 0));
            this.getChildren().addAll(spacer, metaAndBubble);
        } else {
            // Other user: avatar + meta info + bubble (left aligned)
            StackPane avatar = createAvatar(message.getUsername());
            VBox metaAndBubble = createMessageContainer(message, false);

            // Create container to align avatar at bottom of message bubble
            VBox avatarContainer = new VBox();
            avatarContainer.setAlignment(Pos.BOTTOM_LEFT);
            Region avatarSpacer = new Region();
            VBox.setVgrow(avatarSpacer, javafx.scene.layout.Priority.ALWAYS);
            avatarContainer.getChildren().addAll(avatarSpacer, avatar);

            HBox.setMargin(metaAndBubble, new Insets(0, 0, 0, 10));
            this.getChildren().addAll(avatarContainer, metaAndBubble);
        }
    }

    private StackPane createAvatar(String username) {
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(30, 30);
        avatarContainer.setMaxSize(30, 30);
        avatarContainer.setMinSize(30, 30);

        // Background circle
        Circle background = new Circle(15);

        // Generate color based on username hash
        int hash = Math.abs(username.hashCode());
        Color[] colors = {
                Color.web("#FF6B6B"), Color.web("#4ECDC4"), Color.web("#45B7D1"),
                Color.web("#96CEB4"), Color.web("#FECA57"), Color.web("#FF9FF3"),
                Color.web("#54A0FF"), Color.web("#5F27CD"), Color.web("#00D2D3"),
                Color.web("#FF9F43"), Color.web("#10AC84"), Color.web("#EE5A24")
        };

        background.setFill(colors[hash % colors.length]);
        background.setStroke(Color.web("#333333"));
        background.setStrokeWidth(1);

        // User icon
        FontIcon userIcon = new FontIcon(FontAwesomeSolid.USER);
        userIcon.setIconSize(16);
        userIcon.setIconColor(Color.WHITE);

        avatarContainer.getChildren().addAll(background, userIcon);
        HBox.setMargin(avatarContainer, new Insets(0, 8, 0, 0));

        return avatarContainer;
    }

    private VBox createMessageContainer(Message message, boolean isCurrentUser) {
        VBox container = new VBox(3);
        container.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Meta information (username and time)
        String metaText = message.getUsername() + " • " + message.getFormattedTime();
        Label metaLabel = new Label(metaText);
        metaLabel.setStyle(
                "-fx-text-fill: #888888; " +
                        "-fx-font-size: 10px; " +
                        "-fx-padding: 0 8 2 8;"
        );
        metaLabel.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Message bubble
        Label messageLabel = new Label(message.getContent());
        messageLabel.setWrapText(true);
        messageLabel.setMinWidth(35);
        messageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);

        // Bind to parent ListView width if available
        if (parentListView != null) {
            messageLabel.maxWidthProperty().bind(parentListView.widthProperty().multiply(0.5));
        }

        String bubbleColor = isCurrentUser ? "#00BFA5" : "#3a3a3a";
        String textColor = isCurrentUser ? "white" : "#e0e0e0";

        messageLabel.setStyle(
                "-fx-background-color: " + bubbleColor + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-padding: 9 9; " +
                        "-fx-background-radius: 15; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-family: 'Artifakt Element'; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );

        container.getChildren().addAll(metaLabel, messageLabel);
        return container;
    }



}