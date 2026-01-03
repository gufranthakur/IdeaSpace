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
        String os = System.getProperty("os.name").toLowerCase();
        File workingDir = new File(new File(System.getProperty("user.dir")).getParent(), "python");

        String pythonExe = workingDir.getAbsolutePath() + "\\.venv\\Scripts\\python.exe";
        String scriptPath = "src\\handstuff\\main.py";  // Changed to src\main.py

        pb = new ProcessBuilder(pythonExe, scriptPath);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);

        try {
            System.out.println("Starting Python from: " + pythonExe);
            System.out.println("Script: " + scriptPath);
            process = pb.start();
            System.out.println("Python script started successfully");

            // Read Python output
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Python] " + line);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading Python output: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            System.err.println("Failed to start Python: " + e.getMessage());
            e.printStackTrace();
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
