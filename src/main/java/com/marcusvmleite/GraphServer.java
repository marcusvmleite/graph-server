package com.marcusvmleite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraphServer {

    private ServerSocket serverSocket;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            while (true)
                new EchoClientHandler(serverSocket.accept()).start();

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

    private static class EchoClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String uuid;
        private boolean ack = false;

        public EchoClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.uuid = UUID.randomUUID().toString();
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out.println("HI, I AM " + uuid);
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("HI, I AM")) {
                        Pattern pairRegex = Pattern.compile("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");
                        Matcher matcher = pairRegex.matcher(inputLine);
                        if (matcher.find()) {
                            out.println("HI " + matcher.group(0));
                        }
                    }
                    out.println(inputLine);
                }

                in.close();
                out.close();
                clientSocket.close();

            } catch (IOException e) {
                //LOG.debug(e.getMessage());
            }
        }
    }

}
