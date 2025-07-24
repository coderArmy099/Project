package com.example.project.model;

import java.time.LocalDateTime;

public class Room {
    private String roomName;
    private String hostIP;
    private int port;
    private boolean isPrivate;
    private String password;
    private int duration; // minutes
    private int currentUsers;
    private int maxUsers = 15; // default max
    private LocalDateTime createdTime;

    public Room(String roomName, String hostIP, int port, boolean isPrivate,
                String password, int duration) {
        this.roomName = roomName;
        this.hostIP = hostIP;
        this.port = port;
        this.isPrivate = isPrivate;
        this.password = password;
        this.duration = duration;
        this.currentUsers = 1; // host counts as 1
        this.createdTime = LocalDateTime.now();
    }

    // Getters and setters
    public String getRoomName() { return roomName; }
    public String getHostIP() { return hostIP; }
    public int getPort() { return port; }
    public boolean isPrivate() { return isPrivate; }
    public String getPassword() { return password; }
    public int getDuration() { return duration; }
    public int getCurrentUsers() { return currentUsers; }
    public int getMaxUsers() { return maxUsers; }
    public LocalDateTime getCreatedTime() { return createdTime; }

    public void setCurrentUsers(int currentUsers) { this.currentUsers = currentUsers; }

    // For network transmission
    public String toNetworkString() {
        return String.join("|",
                roomName, hostIP, String.valueOf(port),
                String.valueOf(isPrivate), String.valueOf(duration),
                String.valueOf(currentUsers), String.valueOf(maxUsers));
    }

    public static Room fromNetworkString(String data) {
        String[] parts = data.split("\\|");
        Room room = new Room(parts[0], parts[1], Integer.parseInt(parts[2]),
                Boolean.parseBoolean(parts[3]), "", Integer.parseInt(parts[4]));
        room.setCurrentUsers(Integer.parseInt(parts[5]));
        return room;
    }
}