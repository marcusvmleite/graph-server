package com.marcusvmleite.gs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;

public class GraphServer {

    private static final Logger log = LogManager.getLogger(GraphServer.class);

    private static final int PORT = 50000;

    private ServerSocket serverSocket;
    private Graph graph;

    public GraphServer() {
        this.graph = new Graph();
    }

    public void start() {

        log.info("Starting Graph-Server...");

        try {
            serverSocket = new ServerSocket(PORT);
            log.info("Socket is waiting for client requests...");
            while (true) {
                new Session(serverSocket.accept(), this.graph).start();
            }
        } catch (IOException e) {
            log.error("An error occurred during execution of Graph-Server.", e);
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            log.error("An error occurred while stopping Graph-Server.", e);
        }
    }

}
