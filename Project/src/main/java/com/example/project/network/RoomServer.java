package com.example.project.network;

import com.example.project.model.Message;
import com.example.project.model.Room;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoomServer {
    private Room room;
    private ServerSocket serverSocket;
    private RoomDiscovery roomDiscovery;
    private boolean isRunning = false;

    // Thread-safe collections for client management
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private List<Message> messageHistory = new CopyOnWriteArrayList<>();

    public RoomServer(Room room) {
        this.room = room;
        this.roomDiscovery = new RoomDiscovery();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(room.getPort());
            isRunning = true;

            System.out.println("Room server started on port " + room.getPort());

            // Start broadcasting room discovery
            roomDiscovery.startBroadcasting(room, 3); // Broadcast every 3 seconds

            // Accept client connections
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleNewClient(clientSocket);
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private void handleNewClient(Socket clientSocket) {
        if (room.getCurrentUsers() >= room.getMaxUsers()) {
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("ERROR:Room is full");
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error rejecting client: " + e.getMessage());
            }
            return;
        }

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Read client join request
            String joinRequest = in.readLine();
            if (joinRequest == null) return;

            String[] parts = joinRequest.split(":", 3);
            if (parts.length < 2 || !parts[0].equals("JOIN")) {
                out.println("ERROR:Invalid join request");
                clientSocket.close();
                return;
            }

            String username = parts[1];
            String password = parts.length > 2 ? parts[2] : "";

            // Validate password for private rooms
            if (room.isPrivate() && !room.getPassword().equals(password)) {
                out.println("ERROR:Invalid password");
                clientSocket.close();
                return;
            }

            // Check for duplicate username
            if (clients.containsKey(username)) {
                out.println("ERROR:Username already taken");
                clientSocket.close();
                return;
            }

            // Accept the client
            out.println("SUCCESS:Welcome to " + room.getRoomName());

            // Create client handler
            ClientHandler clientHandler = new ClientHandler(username, clientSocket, in, out);
            clients.put(username, clientHandler);

            // Update room user count
            room.setCurrentUsers(clients.size() + 1); // +1 for host

            // Send message history to new client
            for (Message msg : messageHistory) {
                out.println("MESSAGE:" + msg.toNetworkString());
            }

            // Broadcast user joined
            Message joinMessage = Message.systemMessage(username + " joined the room");
            broadcastMessage(joinMessage);

            // Start handling this client's messages
            Thread clientThread = new Thread(clientHandler);
            clientThread.setDaemon(true);
            clientThread.start();

            System.out.println(username + " joined the room. Total users: " + room.getCurrentUsers());

        } catch (IOException e) {
            System.err.println("Error handling new client: " + e.getMessage());
        }
    }

    public void broadcastMessage(Message message) {
        messageHistory.add(message);
        String messageData = "MESSAGE:" + message.toNetworkString();

        // Send to all connected clients
        for (ClientHandler client : clients.values()) {
            client.sendMessage(messageData);
        }
    }

    public void removeClient(String username) {
        ClientHandler removed = clients.remove(username);
        if (removed != null) {
            room.setCurrentUsers(clients.size() + 1); // +1 for host

            Message leaveMessage = Message.systemMessage(username + " left the room");
            broadcastMessage(leaveMessage);

            System.out.println(username + " left the room. Total users: " + room.getCurrentUsers());
        }
    }

    public void stopServer() {
        isRunning = false;

        // Notify all clients that server is shutting down
        Message shutdownMessage = Message.systemMessage("Room is closing");
        broadcastMessage(shutdownMessage);

        // Close all client connections
        for (ClientHandler client : clients.values()) {
            client.disconnect();
        }
        clients.clear();

        // Stop broadcasting
        if (roomDiscovery != null) {
            roomDiscovery.cleanup();
        }

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        System.out.println("Room server stopped");
    }

    public Room getRoom() { return room; }
    public int getClientCount() { return clients.size(); }

    // Inner class to handle individual client connections
    private class ClientHandler implements Runnable {
        private String username;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private boolean connected = true;

        public ClientHandler(String username, Socket socket, BufferedReader in, PrintWriter out) {
            this.username = username;
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while (connected && (inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("MESSAGE:")) {
                        String messageContent = inputLine.substring(8);
                        Message message = new Message(username, messageContent);
                        broadcastMessage(message);
                    } else if (inputLine.equals("DISCONNECT")) {
                        break;
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Error reading from client " + username + ": " + e.getMessage());
                }
            } finally {
                disconnect();
                removeClient(username);
            }
        }

        public void sendMessage(String message) {
            if (connected && out != null) {
                out.println(message);
            }
        }

        public void disconnect() {
            connected = false;
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
