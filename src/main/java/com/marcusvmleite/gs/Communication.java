package com.marcusvmleite.gs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Communication extends Thread {

    private static final int TIMEOUT = 30000;
    private static final String REGEX_UUID = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";

    private Socket clientSocket;
    private String uuid;
    private String name;
    private Graph graph;

    private PrintWriter out;
    private BufferedReader in;

    public Communication(Socket socket, Graph graph) {
        this.clientSocket = socket;
        this.uuid = UUID.randomUUID().toString();
        this.graph = graph;
    }

    public void run() {
        try {

            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            long start = Instant.now().toEpochMilli();
            Timer timer = createTimer(start);
            out.println(String.format(Messages.GS_001.message(), this.uuid));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                timer.cancel();
                timer = createTimer(start);
                if (inputLine.startsWith("HI") || inputLine.startsWith("BYE")) {
                    if (!processGreetingMessage(start, inputLine)) return;
                } else {
                    processGraphMessage(inputLine);
                }
            }

            finishSocket();

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private void finishSocket() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    private void processGraphMessage(String inputLine) {
        if (inputLine.startsWith(Messages.PREFIX_ADD_NODE.message())) {
            String[] tokens = inputLine.split(" ");
            if (graph.addNode(tokens[2])) {
                out.println(Messages.GS_008.message());
            } else {
                out.println(Messages.GS_009.message());
            }
        } else if (inputLine.startsWith(Messages.PREFIX_ADD_EDGE.message())) {
            String[] tokens = inputLine.split(" ");
            if (graph.addEdge(tokens[2], tokens[3], Integer.valueOf(tokens[4]))) {
                out.println(Messages.GS_0011.message());
            } else {
                out.println(Messages.GS_0012.message());
            }
        } else if (inputLine.startsWith(Messages.PREFIX_REMOVE_NODE.message())) {
            String[] tokens = inputLine.split(" ");
            if (graph.removeNode(tokens[2])) {
                out.println(Messages.NODE_REMOVED.message());
            } else {
                out.println(Messages.GS_0012.message());
            }
        }
    }

    private boolean processGreetingMessage(long start, String inputLine) throws IOException {
        boolean result = true;
        if (inputLine.startsWith(Messages.GS_000.message())) {
            Pattern pairRegex = Pattern.compile(REGEX_UUID);
            Matcher matcher = pairRegex.matcher(inputLine);
            if (matcher.find()) {
                this.name = matcher.group(0);
                out.println(String.format(Messages.GS_002.message(), this.name));
            }
        } else if (Messages.GS_003.message().equals(inputLine)) {
            long end = Instant.now().toEpochMilli();
            out.println(String.format(Messages.GS_004.message(), this.name, end - start));
            finishSocket();
            result = false;
        } else {
            out.println(Messages.GS_005.message());
        }
        return result;
    }

    private Timer createTimer(long start) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long end = Instant.now().toEpochMilli();
                out.println(String.format(Messages.GS_004.message(), name, end - start));
                try {
                    finishSocket();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, TIMEOUT, TIMEOUT);
        return timer;
    }

}
