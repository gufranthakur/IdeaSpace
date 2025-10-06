package com.ideaspace.core;

import com.ideaspace.IdeaSpace;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Server implements Runnable{

    private IdeaSpace ideaSpace;
    private HttpServer httpServer;
    public Server(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    private void createServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(65000), 0);
            httpServer.setExecutor(null);

            httpServer.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    byte[] bytes = exchange.getRequestBody().readAllBytes();
                    String command = new String(bytes);

                    ideaSpace.decoder.decode(command);

                    try (OutputStream outputStream = exchange.getResponseBody()) {
                        outputStream.write(command.getBytes());
                    }

                }
            });

        } catch (IOException e) {
            System.out.println("Exception occurred : " + e.getMessage());
            stopServer();
        }
    }

    public void startServer() {
        if (httpServer != null) {
            httpServer.start();
            System.out.println("HTTP Server started on port 65000");
        } else {
            System.out.println("No existing server found. Creating a new one...");
            createServer();
            httpServer.start();
            System.out.println("HTTP Server started on port 8080");
        }
    }

    public void stopServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            System.out.println("HTTP Server stopped");
        }
    }

    @Override
    public void run() {
        createServer();
        startServer();
    }
}
