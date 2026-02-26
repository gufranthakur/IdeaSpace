package com.ideaspace.models;

import com.ideaspace.IdeaSpace;
import java.io.*;
import java.net.*;

public class Server implements Runnable {

    private IdeaSpace ideaSpace;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private String scriptPath;
    private int port;
    private boolean debugFlag;

    ProcessBuilder pb;
    Process process;

    public Server(IdeaSpace ideaSpace, String scriptPath, int port, boolean debugFlag) {
        this.ideaSpace = ideaSpace;
        this.scriptPath = scriptPath;
        this.port = port;

        this.debugFlag = debugFlag;
    }

    public void startServer() {
        startPythonScript(debugFlag);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Socket Server started on port " + port);

            while (running) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getInetAddress());
                new Thread(() -> handleClient(client)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
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
            System.out.println("Socket stopped");
        }

        stopPythonScript();

        System.out.println("Socket Server stopped");
    }

    private void startPythonScript(boolean debugFlag) {

        if (debugFlag) {
            pb = new ProcessBuilder("venv/bin/python", scriptPath , "--debug");
        } else {
            pb = new ProcessBuilder("venv/bin/python", scriptPath);

        }

        pb.inheritIO();

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
