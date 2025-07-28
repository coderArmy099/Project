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

public class MessageBubble extends HBox {
    private static final String CURRENT_USER = CurrentUser.getUsername(); // You'll need to implement this

    public MessageBubble(Message message) {
        super();

        if (message.isSystemMessage()) {
            createSystemMessage(message);
        } else {
            boolean isCurrentUser = message.getUsername().equals(CURRENT_USER);
            createUserMessage(message, isCurrentUser);
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

    private void createUserMessage(Message message, boolean isCurrentUser) {
        this.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        this.setPrefWidth(Region.USE_COMPUTED_SIZE);
        this.setMaxWidth(Region.USE_COMPUTED_SIZE);

        if (isCurrentUser) {
            // Current user: meta info + bubble (right aligned)
            VBox metaAndBubble = createMessageContainer(message, true);
            Region spacer = new Region();
            StackPane avatar = createAvatar(message.getUsername(), true);
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            this.getChildren().addAll(spacer, metaAndBubble,avatar);
        } else {
            // Other user: avatar + meta info + bubble (left aligned)
            StackPane avatar = createAvatar(message.getUsername(),false); // Changed from Circle to StackPane
            VBox metaAndBubble = createMessageContainer(message, false);
            this.getChildren().addAll(avatar, metaAndBubble);
        }
    }


    private StackPane createAvatar(String username, boolean isCurrentUser) {
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(30, 30);
        avatarContainer.setMaxSize(30, 30);
        avatarContainer.setMinSize(30, 30);

        // Background circle
        Circle background = new Circle(17);

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


        if (isCurrentUser) {
            HBox.setMargin(avatarContainer, new Insets(0, 0, 0, 12)); // Left margin for right-aligned
        } else {
            HBox.setMargin(avatarContainer, new Insets(0, 8, 0, 0)); // Right margin for left-aligned
        }

        return avatarContainer;
    }


    private VBox createMessageContainer(Message message, boolean isCurrentUser) {
        VBox container = new VBox(3);
        container.setMaxWidth(300);
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
        messageLabel.setMaxWidth(280);

        String bubbleColor = isCurrentUser ? "#00BFA5" : "#3a3a3a";
        String textColor = isCurrentUser ? "white" : "#e0e0e0";

        messageLabel.setStyle(
                "-fx-background-color: " + bubbleColor + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-padding: 8 12; " +
                        "-fx-background-radius: 15; " +
                        "-fx-font-size: 13px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );

        container.getChildren().addAll(metaLabel, messageLabel);
        return container;
    }
}