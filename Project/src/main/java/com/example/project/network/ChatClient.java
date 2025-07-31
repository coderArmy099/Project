package com.example.project.network;

import com.example.project.model.Message;
import com.example.project.model.Room;
import com.example.project.model.SharedFile;
import com.example.project.network.FileTransferClient;

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
    private FileTransferClient fileTransferClient;

    // Callbacks
    private Consumer<Message> onMessageReceived;
    private Consumer<List<String>> onUserListUpdated;
    private Consumer<Boolean> onConnectionStatusChanged;
    private Consumer<String> onError;
    private Consumer<String> onServerShutdown;
    private Consumer<String> onKicked;
    private Consumer<Boolean> onMuted;
    private Consumer<Integer> onRemainingTimeReceived;

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
            out.println(joinRequest); // server expects "JOIN:username:password

            // Waits for the SUCCESS or ERROR response , SUCCESS hoile dhukay dibe
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

            if (response.startsWith("KICKED:")) {
                String reason = response.substring(7);
                if (onKicked != null) {
                    onKicked.accept(reason);
                }
                return false;
            }

            if (response.startsWith("SUCCESS:")) {
                connected = true;
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(true);
                }

                // Start listening for messages, background e cholbe along with others
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
            connectedUsers.add(username); // Add current user

            while (connected && (inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("MESSAGE:")) {
                    String messageData = inputLine.substring(8);
                    Message message = Message.fromNetworkString(messageData);

                    // Update user list based on system messages
                    if (message.isSystemMessage()) {
                        String content = message.getContent();
                        if (content.contains(" joined the room")) {
                            String joinedUser = content.split(" joined the room")[0];
                            if (!connectedUsers.contains(joinedUser)) {// system message e kew join korse ashle, users list e add hobe
                                connectedUsers.add(joinedUser);// ar , jokhon chatclient join korbe, tokhon to previous messages load kore dibe
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
                }else if (inputLine.startsWith("KICKED:")) {
                    String reason = inputLine.substring(7);
                    if (onKicked != null) {
                        onKicked.accept(reason);
                    }
                    break;
                } else if (inputLine.startsWith("MUTED:")) {
                    if (onMuted != null) {
                        onMuted.accept(true);
                    }
                } else if (inputLine.startsWith("UNMUTED:")) {
                    if (onMuted != null) {
                        onMuted.accept(false);
                    }
                } else if (inputLine.startsWith("SERVER_SHUTDOWN:")) {
                    String reason = inputLine.substring(16);
                    if (onServerShutdown != null) {
                        onServerShutdown.accept(reason);
                    }
                    break;
                }else if(inputLine.startsWith("SESSION_TIMEOUT:")) {
                    String reason = inputLine.substring(16);
                    if (onServerShutdown != null) {
                        onServerShutdown.accept("Session timeout: " + reason);
                    }
                    break;
                }else if(inputLine.startsWith("FILE_PORT:")) {
                    int filePort = Integer.parseInt(inputLine.substring(10));
                    initializeFileTransfer(filePort);
                    //continue;
                }else if (inputLine.startsWith("REMAINING_TIME:")) {
                    int remainingSeconds = Integer.parseInt(inputLine.substring(15));
                    if (onRemainingTimeReceived != null) {
                        onRemainingTimeReceived.accept(remainingSeconds);
                    }
                }
            }
        } catch (IOException e) {
            if (connected && onError != null) {
                onError.accept("Connection lost: " + e.getMessage());
                // Also trigger server shutdown callback for navigation
                if (onServerShutdown != null) {
                    onServerShutdown.accept("Connection lost to server");
                }
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

    public void setOnServerShutdown(Consumer<String> callback) {
        this.onServerShutdown = callback;
    }

    public void setOnKicked(Consumer<String> callback) {
        this.onKicked = callback;
    }

    public void setOnMuted(Consumer<Boolean> callback) {
        this.onMuted = callback;
    }

    public void setOnRemainingTimeReceived(Consumer<Integer> callback) {this.onRemainingTimeReceived = callback;}

    public void initializeFileTransfer(int filePort) {
        this.fileTransferClient = new FileTransferClient(room.getHostIP(), filePort);
    }

    public boolean uploadFile(File file) {
        if (fileTransferClient != null) {
            return fileTransferClient.uploadFile(file, username);
        }
        return false;
    }

    public boolean downloadFile(String fileId, File destination) {
        if (fileTransferClient != null) {
            return fileTransferClient.downloadFile(fileId, destination);
        }
        return false;
    }
}