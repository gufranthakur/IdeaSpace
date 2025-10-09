package com.ideaspace.core;

import com.ideaspace.IdeaSpace;
import java.io.*;
import java.net.*;

public class Server implements Runnable {

    private IdeaSpace ideaSpace;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public Server(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(65000);
            System.out.println("Socket Server started on port 65000");

            while (running) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getInetAddress());
                new Thread(() -> handleClient(client)).start();
            }

        } catch (IOException e) {
            System.out.println("Exception occurred: " + e.getMessage());
        }
    }

    private void handleClient(Socket client) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))
        ) {
            String command;
            while ((command = in.readLine()) != null) {
                ideaSpace.decoder.decode(command);  // handle incoming command
                out.write("ACK\n");
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Client disconnected");
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Socket Server stopped");
    }

    @Override
    public void run() {
        startServer();
    }
}
