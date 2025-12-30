package com.ideaspace.core;

import com.ideaspace.IdeaSpace;
import java.io.*;
import java.net.*;

public class Server implements Runnable {

    private IdeaSpace ideaSpace;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    ProcessBuilder pb;
    Process process;

    public Server(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    public void startServer() {
        startPythonScript();

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
                ideaSpace.decoder.decode(command);
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

        stopPythonScript();

        System.out.println("Socket Server stopped");
    }

    private void startPythonScript() {
        pb = new ProcessBuilder("venv/bin/python", "src/handstuff/main.py");
        pb.redirectErrorStream(true);

        try {
            pb.directory(new File("../python"));
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void stopPythonScript() {
        if (process != null && process.isAlive()) {
            System.out.println("Stopping Python process...");

            process.destroy();

            try {
                boolean exited = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

                if (!exited) {
                    System.out.println("Process didn't stop gracefully, forcing termination...");
                    process.destroyForcibly();
                    process.waitFor();
                }

                System.out.println("Python process stopped");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    @Override
    public void run() {
        startServer();
    }
}
