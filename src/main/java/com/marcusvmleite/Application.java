package com.marcusvmleite;

import com.marcusvmleite.gs.GraphServer;

public class Application {

    public static void main(String[] args) {
        GraphServer server = new GraphServer();
        server.start();
    }

}
