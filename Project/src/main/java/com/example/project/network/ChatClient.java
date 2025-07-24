package com.example.project.network;

import com.example.project.model.Message;
import com.example.project.model.Room;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatClient {
    private Room room;
    private String username;
    private String password;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;

    // Callbacks
    private Consumer<Message> onMessageReceived;
    private Consumer<List<String>> onUserListUpdated;
    private Consumer<Boolean> onConnectionStatusChanged;
    private Consumer<String> onError;

    public ChatClient(Room room, String username, String password) {
        this.room = room;
        this.username = username;
        this.password = password;
    }

    public boolean connect() {
        try {
            socket = new Socket(room.getHostIP(), room.getPort());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send join request
            String joinRequest = "JOIN:" + username;
            if (room.isPrivate() && password != null && !password.isEmpty()) {
                joinRequest += ":" + password;
            }
            out.println(joinRequest);

            // Wait for response
            String response = in.readLine();
            if (response == null) {
                if (onError != null) onError.accept("No response from server");
                return false;
            }

            if (response.startsWith("ERROR:")) {
                String error = response.substring(6);
                if (onError != null) onError.accept(error);
                return false;
            }

            if (response.startsWith("SUCCESS:")) {
                connected = true;
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(true);
                }

                // Start listening for messages
                Thread listenerThread = new Thread(this::messageListener);
                listenerThread.setDaemon(true);
                listenerThread.start();

                // Add ourselves to user list
                List<String> users = new ArrayList<>();
                users.add(username);
                if (onUserListUpdated != null) {
                    onUserListUpdated.accept(users);
                }

                return true;
            }

            return false;

        } catch (IOException e) {
            if (onError != null) {
                onError.accept("Connection failed: " + e.getMessage());
            }
            return false;
        }
    }

    private void messageListener() {
        try {
            String inputLine;
            List<String> connectedUsers = new ArrayList<>();
            connectedUsers.add(username); // Add ourselves

            while (connected && (inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("MESSAGE:")) {
                    String messageData = inputLine.substring(8);
                    Message message = Message.fromNetworkString(messageData);

                    // Update user list based on system messages
                    if (message.isSystemMessage()) {
                        String content = message.getContent();
                        if (content.contains(" joined the room")) {
                            String joinedUser = content.split(" joined the room")[0];
                            if (!connectedUsers.contains(joinedUser)) {
                                connectedUsers.add(joinedUser);
                                if (onUserListUpdated != null) {
                                    onUserListUpdated.accept(new ArrayList<>(connectedUsers));
                                }
                            }
                        } else if (content.contains(" left the room")) {
                            String leftUser = content.split(" left the room")[0];
                            connectedUsers.remove(leftUser);
                            if (onUserListUpdated != null) {
                                onUserListUpdated.accept(new ArrayList<>(connectedUsers));
                            }
                        }
                    }

                    if (onMessageReceived != null) {
                        onMessageReceived.accept(message);
                    }
                }
            }
        } catch (IOException e) {
            if (connected && onError != null) {
                onError.accept("Connection lost: " + e.getMessage());
            }
        } finally {
            connected = false;
            if (onConnectionStatusChanged != null) {
                onConnectionStatusChanged.accept(false);
            }
        }
    }

    public void sendMessage(String messageContent) {
        if (connected && out != null) {
            out.println("MESSAGE:" + messageContent);
        }
    }

    public void disconnect() {
        connected = false;

        if (out != null) {
            out.println("DISCONNECT");
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }

        if (onConnectionStatusChanged != null) {
            onConnectionStatusChanged.accept(false);
        }
    }

    // Setters for callbacks
    public void setOnMessageReceived(Consumer<Message> callback) {
        this.onMessageReceived = callback;
    }

    public void setOnUserListUpdated(Consumer<List<String>> callback) {
        this.onUserListUpdated = callback;
    }

    public void setOnConnectionStatusChanged(Consumer<Boolean> callback) {
        this.onConnectionStatusChanged = callback;
    }

    public void setOnError(Consumer<String> callback) {
        this.onError = callback;
    }

    public boolean isConnected() {
        return connected;
    }
}