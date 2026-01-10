package com.ideaspace.simulationhand;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceiver implements Runnable {

    private DatagramSocket socket;
    private volatile String data = "";
    private volatile boolean running = true;
    private Thread thread;

    private final int port;

    public UDPReceiver(int port) {
        this.port = port;
    }

    public void start() {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(1000);
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[4096];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                data = new String(packet.getData(), 0, packet.getLength());
            } catch (Exception e) {
                // Timeout or error, continue
            }
        }
    }

    public String getData() {
        return data;
    }

    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}
