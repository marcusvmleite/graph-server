package com.marcusvmleite.gs;

import java.io.IOException;
import java.net.ServerSocket;

public class GraphServer {

    private static final int PORT = 50000;

    private ServerSocket serverSocket;
    private Graph graph;

    public GraphServer() {
        this.graph = new Graph();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            while (true) {
                new Communication(serverSocket.accept(), this.graph).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
