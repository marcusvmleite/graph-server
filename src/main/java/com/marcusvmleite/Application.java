package com.marcusvmleite;

public class Application {

    public static void main(String[] args) {
        GraphServer server = new GraphServer();
        server.start(50000);
    }

}
