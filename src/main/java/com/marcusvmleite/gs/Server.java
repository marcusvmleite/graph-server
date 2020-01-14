package com.marcusvmleite.gs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents the Graph-Server. It will listen
 * for Client requests and for each Client a new
 * Session Thread will be created.
 *
 * @author marcusvmleite
 * @since 13.01.2020
 * @version 1.0
 */
public class Server {

    private static final Logger log = LogManager.getLogger(Server.class);

    /**
     * Port for the server.
     */
    private static final int PORT = 50000;

    /**
     * Server Socket.
     */
    private ServerSocket serverSocket;

    /**
     * Starts the Graph-Server.
     */
    public void start() {
        log.info("Starting Graph-Server...");
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            serverSocket = new ServerSocket(PORT);
            log.info("Socket is waiting for client requests...");
            while (true) {
                executor.submit(new Session(serverSocket.accept(), Graph.getInstance()));
            }
        } catch (IOException e) {
            log.error("An error occurred during execution of Graph-Server.", e);
        } finally {
            executor.shutdown();
            stop();
        }
    }

    /**
     * Stops the Graph-Server.
     */
    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            log.error("An error occurred while stopping Graph-Server.", e);
        }
    }

}
