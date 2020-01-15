package com.marcusvmleite;

import com.marcusvmleite.gs.Server;

/**
 * Graph Server's Main Class.
 * Only purpose is to start the {@link Server}.
 *
 * @author marcusvmleite
 * @since 13.01.2020
 * @version 1.0
 */
public class Application {

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

}
