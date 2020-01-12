package com.marcusvmleite.gs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Session extends Thread {

    private static final Logger log = LogManager.getLogger(Session.class);

    private static final int TIMEOUT = 30000;
    private static final String REGEX_UUID = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";

    private Socket clientSocket;
    private String sessionId;
    private String clientId;
    private Graph graph;

    private PrintWriter out;
    private BufferedReader in;

    public Session(Socket socket, Graph graph) {
        this.clientSocket = socket;
        this.sessionId = UUID.randomUUID().toString();
        this.graph = graph;
    }

    public void run() {

        long start = Instant.now().toEpochMilli();

        try {

            this.clientSocket.setSoTimeout(TIMEOUT);
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            log.info("Session [{}] is now active.", this.sessionId);
            out.println(String.format(Messages.GS_001.message(), this.sessionId));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("HI") || inputLine.startsWith("BYE")) {
                    if (!processGreetingMessage(start, inputLine)) {
                        break;
                    }
                } else {
                    processGraphMessage(inputLine);
                }
            }

        } catch (IOException e) {
            log.error("An error occurred during Session [{}].", this.sessionId, e);
            if (e instanceof SocketTimeoutException) {
                long end = Instant.now().toEpochMilli();
                long total = end - start;
                log.info("Session [{}] with Client [{}] finished after [{}] ms.", sessionId, clientId, total);
                out.println(String.format(Messages.GS_004.message(), this.clientId, total));
            }
        } finally {
            finishSocket();
        }
    }

    private void finishSocket() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            log.info("Session [{}] with Client [{}] finished.", this.sessionId, this.clientId);
        } catch (IOException e) {
            log.error("An error occurred while finishing socket for Session [{}].", this.sessionId, e);
        }
    }

    private void processGraphMessage(String inputLine) {
        if (inputLine.startsWith(Messages.PREFIX_ADD_NODE.message())) {
            addNode(inputLine);
        } else if (inputLine.startsWith(Messages.PREFIX_ADD_EDGE.message())) {
            addEdge(inputLine);
        } else if (inputLine.startsWith(Messages.PREFIX_REMOVE_NODE.message())) {
            removeNode(inputLine);
        } else if (inputLine.startsWith(Messages.PREFIX_REMOVE_EDGE.message())) {
            removeEdge(inputLine);
        }
    }

    private void removeEdge(String inputLine) {
        String[] tokens = inputLine.split(" ");
        String from = tokens[2];
        String to = tokens[3];
        log.info("Removing Edge from Node [{}] to Node [{}].", from, to);
        if (graph.removeEdge(tokens[2], tokens[3])) {
            out.println(Messages.EDGE_REMOVED.message());
        } else {
            log.warn("Could not remove Edge from Node [{}] to Node [{}] because one of " +
                    "the Nodes do not exists.", from, to);
            out.println(Messages.GS_0012.message());
        }
    }

    private void removeNode(String inputLine) {
        String[] tokens = inputLine.split(" ");
        String name = tokens[2];
        log.info("Removing Node with name [{}].", name);
        if (graph.removeNode(name)) {
            out.println(Messages.NODE_REMOVED.message());
        } else {
            log.warn("Could not remove Node with name [{}] because it do not exists.", name);
            out.println(Messages.GS_0012.message());
        }
    }

    private void addEdge(String inputLine) {
        String[] tokens = inputLine.split(" ");
        String from = tokens[2];
        String to = tokens[3];
        int weight = Integer.valueOf(tokens[4]);
        log.info("Adding Edge from Node [{}] to Node [{}] with Weight [{}].", from, to, weight);
        if (graph.addEdge(tokens[2], tokens[3], Integer.valueOf(tokens[4]))) {
            out.println(Messages.GS_0011.message());
        } else {
            log.warn("Could not add Edge from Node [{}] to Node [{}] with Weight [{}] because one of " +
                    "the Nodes do not exists.", from, to, weight);
            out.println(Messages.GS_0012.message());
        }
    }

    private void addNode(String inputLine) {
        String[] tokens = inputLine.split(" ");
        String name = tokens[2];
        log.info("Adding Node with name [{}].", name);
        if (graph.addNode(name)) {
            out.println(Messages.GS_008.message());
        } else {
            log.warn("Could not add Node with name [{}] because it already exists.", name);
            out.println(Messages.GS_009.message());
        }
    }

    private boolean processGreetingMessage(long start, String inputLine) {
        boolean result = true;
        if (inputLine.startsWith(Messages.GS_000.message())) {
            Pattern pairRegex = Pattern.compile(REGEX_UUID);
            Matcher matcher = pairRegex.matcher(inputLine);
            if (matcher.find()) {
                this.clientId = matcher.group(0);
                log.info("Session [{}] received greeting from Client [{}].", this.sessionId, this.clientId);
                out.println(String.format(Messages.GS_002.message(), this.clientId));
            }
        } else if (Messages.GS_003.message().equals(inputLine)) {
            long end = Instant.now().toEpochMilli();
            long total = end - start;
            log.info("Session [{}] with Client [{}] finished after [{}] ms.", sessionId, clientId, total);
            out.println(String.format(Messages.GS_004.message(), this.clientId, total));
            result = false;
        } else {
            log.warn("Session received a message that could not be recognized. Message was: [{}].", inputLine);
            out.println(Messages.GS_005.message());
        }
        return result;
    }

}
