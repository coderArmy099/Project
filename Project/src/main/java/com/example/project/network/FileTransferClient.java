
package com.example.project.network;

import java.io.*;
import java.net.Socket;

public class FileTransferClient {
    private String serverIP;
    private int filePort;

    public FileTransferClient(String serverIP, int filePort) {
        this.serverIP = serverIP;
        this.filePort = filePort;
    }

    public boolean uploadFile(File file, String username) {
        try (Socket socket = new Socket(serverIP, filePort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {

            String fileId = java.util.UUID.randomUUID().toString();
            // Send request as a single UTF string
            out.writeUTF("UPLOAD");
            out.writeUTF(fileId);
            out.writeUTF(file.getName());
            out.writeLong(file.length());
            out.writeUTF(username);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

            return true;

        } catch (IOException e) {
            System.err.println("Error uploading file: " + e.getMessage());
            return false;
        }
    }

    public boolean downloadFile(String fileId, File destinationFile) {
        try (Socket socket = new Socket(serverIP, filePort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Send request
            out.writeUTF("DOWNLOAD");
            out.writeUTF(fileId);
            out.flush();

            // Read response
            String response = in.readUTF();
            if (!"OK".equals(response)) {
                System.err.println("Download failed: " + response);
                destinationFile.delete(); // Clean up empty file
                return false;
            }

            long fileSize = in.readLong();
            // The original name is not strictly needed here but reading it to keep stream sync
            in.readUTF();

            try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
                byte[] buffer = new byte[8192];
                long totalReceived = 0;

                while (totalReceived < fileSize) {
                    int toRead = (int) Math.min(buffer.length, fileSize - totalReceived);
                    int bytesRead = in.read(buffer, 0, toRead);
                    if (bytesRead == -1) break;

                    fos.write(buffer, 0, bytesRead);
                    totalReceived += bytesRead;
                }
                return totalReceived == fileSize;
            }

        } catch (IOException e) {
            System.err.println("Error downloading file: " + e.getMessage());
            destinationFile.delete(); // Clean up partial file
            return false;
        }
    }
}