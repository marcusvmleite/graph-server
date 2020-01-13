package com.marcusvmleite.gs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {

    private static final Logger log = LogManager.getLogger(Server.class);

    private static final int PORT = 50000;

    private ServerSocket serverSocket;

    public Server() {}

    public void start() {

        log.info("Starting Graph-Server...");

        try {
            serverSocket = new ServerSocket(PORT);
            log.info("Socket is waiting for client requests...");
            while (true) {
                new Session(serverSocket.accept(), Graph.getInstance()).start();
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
