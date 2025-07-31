package com.example.project.network;

import com.example.project.model.Message;
import com.example.project.model.Room;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.example.project.model.SharedFile;
import com.example.project.network.FileTransferServer;


public class RoomServer {
    private Room room;
    private ServerSocket serverSocket;
    private RoomDiscovery roomDiscovery;
    private boolean isRunning = false;
    private Timer sessionTimer;
    private FileTransferServer fileTransferServer;
    private long sessionStartTime;
    private int sessionDurationMinutes;

    // Thread-safe collections for client management
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private List<Message> messageHistory = new CopyOnWriteArrayList<>();

    private Set<String> mutedUsers = ConcurrentHashMap.newKeySet();

    private Map<String, SharedFile> sharedFiles = new ConcurrentHashMap<>();
    private String filesDirectory;

    public RoomServer(Room room) {
        this.room = room;
        this.roomDiscovery = new RoomDiscovery();
        this.filesDirectory = "room_files_" + room.getPort();
        this.sessionDurationMinutes = room.getDuration();
        this.sessionStartTime = System.currentTimeMillis();
        createFilesDirectory();

        this.fileTransferServer = new FileTransferServer(this);
        this.fileTransferServer.startFileServer();

    }

    private void createFilesDirectory() {
        File dir = new File(filesDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public int getFilePort() {
        return fileTransferServer != null ? fileTransferServer.getFilePort() : -1;
    }


    public boolean checkUserExists(String username) {
        return clients.containsKey(username);
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(room.getPort());
            isRunning = true;

            System.out.println("Room server started on port " + room.getPort());
            startSessionTimer();
            // Start broadcasting room discovery
            roomDiscovery.startBroadcasting(room, 3); // Broadcast every 3 seconds

            // Accept client connections
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept(); // runs in background thread
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

    private void startSessionTimer() {
        sessionTimer = new Timer(true);
        long durationMillis = room.getDuration() * 60 * 1000L;

        sessionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message timeoutMessage = Message.systemMessage("Session time expired. Room is closing.");
                broadcastMessage(timeoutMessage);

                //sessiontimeout message
                for (ClientHandler client : clients.values()) {
                    client.sendMessage("SESSION_TIMEOUT:Session ended");
                }

                Timer shutdownTimer = new Timer();
                shutdownTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        stopServer();
                    }
                }, 2000);
            }
        }, durationMillis);
    }

    public int getRemainingTimeSeconds() {
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - sessionStartTime) / 1000;
        long totalSeconds = sessionDurationMinutes * 60L;
        return Math.max(0, (int)(totalSeconds - elapsedSeconds));
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

            // duplicate user handling(ekhon apatoto join korte dibo na)
            if (clients.containsKey(username)) {
                out.println("KICKED:Username already taken or joined from a different device, leave the room first from the other device");
                clientSocket.close();
                return;
            }

            // Accept the client
            out.println("SUCCESS:Welcome to " + room.getRoomName());
            out.println("FILE_PORT:" + getFilePort());
            out.println("REMAINING_TIME:" + getRemainingTimeSeconds());
            // Create clientHandler
            ClientHandler clientHandler = new ClientHandler(username, clientSocket, in, out);
            clients.put(username, clientHandler);

            // Update room user count
            room.setCurrentUsers(clients.size() + 1); // +1 for host

            // previous messages pathailam (messageHistory te store thakar jonno database lagtesena)
            for (Message msg : messageHistory) {
                out.println("MESSAGE:" + msg.toNetworkString());
            }

            // Broadcast user joined
            Message joinMessage = Message.systemMessage(username + " joined the room");
            broadcastMessage(joinMessage);

            // background thread e client handling hobe
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



    public void kickUser(String username) {
        ClientHandler client = clients.get(username);
        if (client != null) {
            client.sendMessage("KICKED:You have been kicked from the room");
            client.disconnect();
            removeClient(username);
        }
    }

    public void muteUser(String username) {
        if (clients.containsKey(username)) {
            mutedUsers.add(username);
            ClientHandler client = clients.get(username);
            if (client != null) {
                client.sendMessage("MUTED:You have been muted");
            }

            Message muteMessage = Message.systemMessage(username + " has been muted");
            broadcastMessage(muteMessage);
        }
    }

    public void unmuteUser(String username) {
        mutedUsers.remove(username);
        ClientHandler client = clients.get(username);
        if (client != null) {
            client.sendMessage("UNMUTED:You have been unmuted");
        }

        Message unmuteMessage = Message.systemMessage(username + " has been unmuted");
        broadcastMessage(unmuteMessage);
    }

    public boolean isUserMuted(String username) {
        return mutedUsers.contains(username);
    }


    // host leave korle call kora hobe
    public void stopServer() {
        isRunning = false;

        // Notify all clients that server is shutting down
        Message shutdownMessage = Message.systemMessage("Room is closing");
        broadcastMessage(shutdownMessage);

        Timer disconnectTimer = new Timer();
        disconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (ClientHandler client : clients.values()) {
                    client.sendMessage("SERVER_SHUTDOWN:Room ended by host");
                    client.disconnect();
                }
                clients.clear();
            }
        }, 1000); // 1 second delay


        // Stop broadcasting
        if (roomDiscovery != null) {
            roomDiscovery.cleanup();
        }

        if (fileTransferServer != null) {
            fileTransferServer.stopFileServer();
        }

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        System.out.println("Room server stopped");
    }

    public Room getRoom() { return room; }
    public int getClientCount() { return clients.size(); }

    // thread class jeita background(daemon thread) e run kore client er request handle kore
    private class ClientHandler implements Runnable {
        private String username; // client name
        private Socket socket; // the socket that client is connected to
        private BufferedReader in; // input stream
        private PrintWriter out;    // output stream
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

                        if (mutedUsers.contains(username)) {
                            sendMessage("ERROR:You are currently muted");
                            continue;
                        }

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
                out.println(message); // ei user re message pathano hobe, pura clients map iterate korle shobar kache message jabe
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



    public void handleFileUpload(String uploaderUsername, String fileId, String fileName, long fileSize) {
        SharedFile sharedFile = new SharedFile(fileId, fileName, uploaderUsername, fileSize);
        sharedFiles.put(fileId, sharedFile);

        // Broadcast file availability
        Message fileMessage = Message.systemMessage(
                uploaderUsername + " shared a file: " + fileName + " (" + formatFileSize(fileSize) + ")"
        );
        fileMessage.setFileId(fileId);
        broadcastMessage(fileMessage);

        Message fileMessage1 = new Message(uploaderUsername, fileName, false, fileId);
        broadcastMessage(fileMessage1);
    }


    public SharedFile getSharedFile(String fileId) {
        return sharedFiles.get(fileId);
    }

    public String getFilesDirectory() {
        return filesDirectory;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}
