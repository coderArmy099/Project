

package com.example.project.network;

import com.example.project.model.SharedFile;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class FileTransferServer {
    private ServerSocket fileServerSocket;
    private int filePort;
    private boolean isRunning = false;
    private RoomServer roomServer;

    public FileTransferServer(RoomServer roomServer) {
        this.roomServer = roomServer;
        this.filePort = findAvailablePort();
    }

    public void startFileServer() {
        try {
            fileServerSocket = new ServerSocket(filePort);
            isRunning = true;

            Thread serverThread = new Thread(this::handleFileRequests);
            serverThread.setDaemon(true);
            serverThread.start();

        } catch (IOException e) {
            System.err.println("Failed to start file server: " + e.getMessage());
        }
    }

    private void handleFileRequests() {
        while (isRunning) {
            try {
                Socket clientSocket = fileServerSocket.accept();
                Thread handler = new Thread(() -> handleFileRequest(clientSocket));
                handler.setDaemon(true);
                handler.start();
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting file request: " + e.getMessage());
                }
            }
        }
    }

    private void handleFileRequest(Socket clientSocket) {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream())) {
            String action = in.readUTF();

            if ("DOWNLOAD".equals(action)) {
                String fileId = in.readUTF();
                handleFileDownload(fileId, clientSocket);
            } else if ("UPLOAD".equals(action)) {
                String fileId = in.readUTF();
                String fileName = in.readUTF();
                long fileSize = in.readLong();
                String username = in.readUTF();
                handleFileUpload(fileId, fileName, fileSize, username, in);
            }
        } catch (IOException e) {
            // This can happen if a client disconnects abruptly, usually safe to ignore.
            System.err.println("Error handling file request: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void handleFileDownload(String fileId, Socket clientSocket) throws IOException {
        try (DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
            SharedFile sharedFile = roomServer.getSharedFile(fileId);
            if (sharedFile == null) {
                out.writeUTF("ERROR:File not found");
                return;
            }

            File file = new File(roomServer.getFilesDirectory(), fileId);
            if (!file.exists()) {
                out.writeUTF("ERROR:File not available");
                return;
            }

            out.writeUTF("OK");
            out.writeLong(sharedFile.getFileSize());
            out.writeUTF(sharedFile.getOriginalName());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    private void handleFileUpload(String fileId, String fileName, long fileSize, String username, DataInputStream in) {
        File fileToSave = new File(roomServer.getFilesDirectory(), fileId);
        try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
            byte[] buffer = new byte[8192];
            long totalReceived = 0;

            while (totalReceived < fileSize) {
                int toRead = (int) Math.min(buffer.length, fileSize - totalReceived);
                int bytesRead = in.read(buffer, 0, toRead);
                if (bytesRead == -1) break;

                fos.write(buffer, 0, bytesRead);
                totalReceived += bytesRead;
            }

            if (totalReceived == fileSize) {
                roomServer.handleFileUpload(username, fileId, fileName, fileSize);
            } else {
                // Upload failed, delete partial file
                fileToSave.delete();
                System.err.println("File upload incomplete for " + fileName);
            }

        } catch (IOException e) {
            System.err.println("Error during file upload: " + e.getMessage());
            // Clean up partial file on error
            fileToSave.delete();
        }
    }

    private int findAvailablePort() {
        for (int port = 10000; port <= 10999; port++) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                // Try next port
            }
        }
        throw new RuntimeException("No available ports for file transfer");
    }

    public int getFilePort() {
        return filePort;
    }

    public void stopFileServer() {
        isRunning = false;
        try {
            if (fileServerSocket != null && !fileServerSocket.isClosed()) {
                fileServerSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing file server: " + e.getMessage());
        }
    }
}