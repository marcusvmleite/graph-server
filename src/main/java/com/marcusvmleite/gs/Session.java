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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Class that represents a Client's Session (conversation).
 * It processes incoming messages and performs actions
 * depending on its contents.
 *
 * This class extends {@link Thread} so the application
 * is able to handle multiple Session at the same time.
 *
 * Valid incoming messages from the Client are:
 *
 *    - Greeting messages:
 *      - HI, I AM <name>
 *      - BYE MATE
 *
 *    - Graph messages:
 *      - ADD NODE <x>
 *      - ADD EDGE <x> <y> <weight>
 *      - REMOVE NODE <x>
 *      - REMOVE EDGE <x> <y>
 *      - SHORTEST PATH <x> <y>
 *      - CLOSER THAN <weight> <x>
 *
 * For each message above, the server will reply with
 * another one, depending on how the process finished
 * successfully or not.
 *
 * @author marcusvmleite
 * @since 13.01.2020
 * @version 1.0
 */
public class Session extends Thread {

    private static final Logger log = LogManager.getLogger(Session.class);

    /**
     * Timeout for the Socket. After this time
     * of innactivity the Session will be finalised and a final
     * message will be sent to the Client
     * "BYE <name>, WE SPOKE FOR <X> MS"
     *
     * Where <name> is the name of the Client and X is the
     * total time of the conversations in milliseconds.
     */
    private static final int TIMEOUT = 30000;

    /**
     * Socket of the Client.
     */
    private Socket clientSocket;

    /**
     * ID of the Session, represented by a unique {@link UUID}
     */
    private String sessionId;

    /**
     * Client ID received via greeting message "HI, I AM <name>"
     */
    private String clientId;

    /**
     * {@link Graph} object. This object is a Singleton and it is
     * shared between all existent Sessions.
     */
    private Graph graph;

    /**
     * For sending back messages to the Client.
     */
    private PrintWriter out;

    /**
     * For reading messages from the Client.
     */
    private BufferedReader in;

    public Session(Socket socket, Graph graph) {
        this.clientSocket = socket;
        this.sessionId = UUID.randomUUID().toString();
        this.graph = graph;
    }

    public void run() {

        //Mark the starting time of the conversation, for
        //helping calculating the total time of the conversation.
        long start = Instant.now().toEpochMilli();

        try {

            this.clientSocket.setSoTimeout(TIMEOUT);
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            log.info("Session [{}] is now active.", this.sessionId);
            out.println(String.format(Messages.GS_001.message(), this.sessionId));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith(Messages.PREFIX_HI.message()) ||
                        inputLine.startsWith(Messages.PREFIX_BYE.message())) {
                    if (!processGreetingMessage(inputLine)) {
                        break;
                    }
                } else {
                    processGraphMessage(inputLine);
                }
            }

        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                log.warn("Session [{}] finished due to timeout.", this.sessionId);
            } else {
                log.error("An error occurred during Session [{}].", this.sessionId, e);
            }
        } finally {
            finishSocket(start);
        }
    }

    private void finishSocket(long start) {
        try {
            long end = Instant.now().toEpochMilli();
            long total = end - start;
            log.info("Session [{}] with Client [{}] finished after [{}] ms.", sessionId, clientId, total);
            out.println(String.format(Messages.GS_004.message(), this.clientId, total));

            in.close();
            out.close();
            clientSocket.close();
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
        } else if (inputLine.startsWith(Messages.PREFIX_SHORTEST_PATH.message())) {
            shortestPath(inputLine);
        } else if (inputLine.startsWith(Messages.PREFIX_CLOSER_THAN.message())) {
            closerThan(inputLine);
        }
    }

    private void removeEdge(String inputLine) {
        String[] tokens = tokenizeInput(inputLine);
        String from = tokens[2], to = tokens[3];
        log.info("Removing Edge from Node [{}] to Node [{}].", from, to);
        if (graph.removeEdge(from, tokens[3])) {
            out.println(Messages.EDGE_REMOVED.message());
        } else {
            log.warn("Could not remove Edge from Node [{}] to Node [{}] because one of " +
                    "the Nodes do not exists.", from, to);
            out.println(Messages.GS_0012.message());
        }
    }

    private void removeNode(String inputLine) {
        String[] tokens = tokenizeInput(inputLine);
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
        String[] tokens = tokenizeInput(inputLine);
        String from = tokens[2], to = tokens[3];
        int weight = Integer.parseInt(tokens[4]);
        log.info("Adding Edge from Node [{}] to Node [{}] with Weight [{}].", from, to, weight);
        if (graph.addEdge(from, to, weight)) {
            out.println(Messages.GS_0011.message());
        } else {
            log.warn("Could not add Edge from Node [{}] to Node [{}] with Weight [{}] because one of " +
                    "the Nodes do not exists.", from, to, weight);
            out.println(Messages.GS_0012.message());
        }
    }

    private void addNode(String inputLine) {
        String[] tokens = tokenizeInput(inputLine);
        String name = tokens[2];
        log.info("Adding Node with name [{}].", name);
        if (graph.addNode(name)) {
            out.println(Messages.GS_008.message());
        } else {
            log.warn("Could not add Node with name [{}] because it already exists.", name);
            out.println(Messages.GS_009.message());
        }
    }

    private void shortestPath(String inputLine) {
        String[] tokens = tokenizeInput(inputLine);
        String from = tokens[2], to = tokens[3];
        log.info("Getting Shortest Path from Node [{}] to Node [{}].", from, to);
        Integer path = graph.shortestPath(from, to);
        if (path == -1) {
            log.warn("Could not get Shortest Path from Node [{}] to Node [{}] because one of " +
                    "the Nodes do not exists.", from, to);
            out.println(Messages.GS_0012.message());
        } else {
            log.info("Shortest Path from Node [{}] to Node [{}] is [{}].", from, to, path);
            out.println(path);
        }
    }

    private void closerThan(String inputLine) {
        String[] tokens = tokenizeInput(inputLine);
        String weight = tokens[2], to = tokens[3];
        log.info("Getting Nodes Closer Than [{}] to Node [{}].", weight, to);
        List<String> result = graph.closerThan(Integer.parseInt(weight), to);
        if (Objects.isNull(result)) {
            log.warn("Could not get Nodes Closer Than [{}] to Node [{}] because this Node does not exists.",
                    weight, to);
            out.println(Messages.GS_0012.message());
        } else if (result.isEmpty()) {
            log.warn("Could not get Nodes Closer Than [{}] to Node [{}].", weight, to);
            out.println("");
        } else{
            out.println(String.join(",", result));
        }
    }

    /**
     * Method responsible for processing a greeting message.
     * A greeting message is a message that starts with HI or BYE.
     *
     * @param inputLine String with the message.
     * @return true if the conversation will continue, false otherwise.
     */
    private boolean processGreetingMessage(String inputLine) {
        boolean continueConversation = true;
        if (inputLine.startsWith(Messages.GS_000.message())) {
            String[] tokens = tokenizeInput(inputLine);
            this.clientId = tokens[3];
            log.info("Session [{}] received greeting from Client [{}].", this.sessionId, this.clientId);
            out.println(String.format(Messages.GS_002.message(), this.clientId));
        } else if (Messages.GS_003.message().equals(inputLine)) {
            continueConversation = false;
        } else {
            log.warn("Session received a message that could not be recognized. Message was: [{}].", inputLine);
            out.println(Messages.GS_005.message());
        }
        return continueConversation;
    }

    /**
     * Tokenize the input providing an array of String objects.
     *
     * @param inputLine Input to be tokenized.
     * @return Array of String.
     */
    private String[] tokenizeInput(String inputLine) {
        return inputLine.split(" ");
    }

}
