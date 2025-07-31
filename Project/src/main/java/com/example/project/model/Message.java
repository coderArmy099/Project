package com.example.project.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private String username;
    private String content;
    private LocalDateTime timestamp;
    private boolean isSystemMessage;
    private String fileId;
    private boolean isFileMessage;

    public Message(String username, String content, boolean isSystemMessage) {
        this.username = username;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.isSystemMessage = isSystemMessage;
    }

    public Message(String username, String content, boolean isSystemMessage, String fileId) {
        this(username, content, isSystemMessage);
        this.fileId = fileId;
        this.isFileMessage = fileId != null;
    }

    public Message(String username, String content) {
        this(username, content, false);
    }

    // (user joined/left, etc.)
    public static Message systemMessage(String content) {
        return new Message("System", content, true);
    }

    // Getters
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSystemMessage() { return isSystemMessage; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) {
        this.fileId = fileId;
        this.isFileMessage = fileId != null;
    }
    public boolean isFileMessage() { return isFileMessage; }

    // Format timestamp for display
    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // For network transmission
    public String toNetworkString() {
        return String.join("|",
                username, content, timestamp.toString(), String.valueOf(isSystemMessage), fileId!=null ? fileId: "");
    }

    public static Message fromNetworkString(String data) {
        String[] parts = data.split("\\|", 5);
        Message message = new Message(parts[0], parts[1], Boolean.parseBoolean(parts[3]));
        message.timestamp = LocalDateTime.parse(parts[2]);
        if (parts.length > 4 && !parts[4].isEmpty()) {
            message.setFileId(parts[4]);
            message.isFileMessage = true;
        }
        return message;
    }
}