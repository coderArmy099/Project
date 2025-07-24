package com.example.project.network;

import com.example.project.model.Room;

public class TestServer {
    public static void main(String[] args) {
        // Create a test room
        Room testRoom = new Room("Study Group #1", "192.168.1.100", 9000, false, "", 90);
        testRoom.setCurrentUsers(3);

        // Start broadcasting
        RoomDiscovery discovery = new RoomDiscovery();
        discovery.startBroadcasting(testRoom, 5); // Broadcast every 5 seconds

        System.out.println("Test server broadcasting room: " + testRoom.getRoomName());

        // Keep the program running
        try {
            Thread.sleep(300000); // Run for 5 minutes
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        discovery.cleanup();
    }
}