package com.example.project.network;

import com.example.project.model.Room;
import java.net.*;
import java.io.IOException;
import java.util.function.Consumer;

public class RoomDiscovery {
    private static final int DISCOVERY_PORT = 8888;
    private static final String BROADCAST_ADDRESS = "255.255.255.255";
    private DatagramSocket broadcastSocket;
    private DatagramSocket listenSocket;
    private boolean isListening = false;
    private Thread listenerThread;

    // Start listening for room broadcasts
    public void startListening(Consumer<Room> onRoomFound) {
        if (isListening) return;

        try {
//            listenSocket = new DatagramSocket(DISCOVERY_PORT);
//            listenSocket.setBroadcast(true);
//            isListening = true;

            DatagramSocket socket = new DatagramSocket(null);
            // allow address reuse so multiple JVMs can bind the same port
            socket.setReuseAddress(true);
            // bind to discovery port
            socket.bind(new InetSocketAddress(DISCOVERY_PORT));
            socket.setBroadcast(true);

            listenSocket = socket;
            isListening = true;

            listenerThread = new Thread(() -> {
                byte[] buffer = new byte[1024];

                while (isListening && !Thread.currentThread().isInterrupted()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        listenSocket.receive(packet);

                        String data = new String(packet.getData(), 0, packet.getLength());

                        // Parse the room data
                        if (data.startsWith("ROOM:")) {
                            String roomData = data.substring(5);
                            Room room = Room.fromNetworkString(roomData);

                            String HostIP = packet.getAddress().getHostAddress();
                            Room updatedRoom = new Room(
                                    room.getRoomName(),
                                    HostIP,
                                    room.getPort(),
                                    room.isPrivate(),
                                    room.getPassword(),
                                    room.getDuration()
                            );
                            updatedRoom.setCurrentUsers(room.getCurrentUsers());
                            onRoomFound.accept(updatedRoom);
                        }

                    } catch (IOException e) {
                        if (isListening) {
                            System.err.println("Error receiving broadcast: " + e.getMessage());
                        }
                    }
                }
            });

            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (IOException e) {
            System.err.println("Failed to start listening: " + e.getMessage());
        }
    }

    // Broadcast a room to the network
    public void broadcastRoom(Room room) {
        try {
            if (broadcastSocket == null) {
                broadcastSocket = new DatagramSocket();
                broadcastSocket.setBroadcast(true);
            }

            String message = "ROOM:" + room.toNetworkString();
            byte[] data = message.getBytes();

            InetAddress broadcast = InetAddress.getByName(BROADCAST_ADDRESS);
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, DISCOVERY_PORT);

            broadcastSocket.send(packet);

        } catch (IOException e) {
            System.err.println("Failed to broadcast room: " + e.getMessage());
        }
    }

    // Start periodic broadcasting for a hosted room
    public void startBroadcasting(Room room, int intervalSeconds) {
        Thread broadcastThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                broadcastRoom(room);
                try {
                    Thread.sleep(intervalSeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        broadcastThread.setDaemon(true); // this thread was made a daemon thread to work in the backgroind (if only daemon theads are running then the JVM will ext)
        broadcastThread.start();
    }

    // Stop listening for broadcasts
    public void stopListening() {
        isListening = false;

        if (listenerThread != null) {
            listenerThread.interrupt();
        }

        if (listenSocket != null && !listenSocket.isClosed()) {
            listenSocket.close();
        }
    }

    // Cleanup all resources
    public void cleanup() {
        stopListening();

        if (broadcastSocket != null && !broadcastSocket.isClosed()) {
            broadcastSocket.close();
        }
    }

    // Get local IP address for hosting
    public static String getLocalIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}