package com.marcusvmleite.gs;

import javafx.util.Pair;
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
import java.util.regex.Pattern;

/**
 * Class that represents a Client's Session (conversation).
 * It processes incoming messages and performs actions
 * depending on its contents.
 *
 * This class extends {@link Thread} so the application
 * is able to handle multiple Sessions at the same time.
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
     * of inactivity the Session will be finalised and a final
     * message will be sent to the Client:
     * "BYE <name>, WE SPOKE FOR <X> MS"
     *
     * Where <name> is the name of the Client and X is the
     * total time of the conversation in milliseconds.
     */
    private static final int TIMEOUT = 30000;

    /**
     * Regular Expressions for Input Pattern Matching.
     */
    private static final String CLIENT_REGEX = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    private static final String NUMERIC_REGEX = "([0-9]+)";
    private static final String NAME_REGEX = "[A-Za-z0-9_-]+";

    /**
     * Patterns for recognizing incoming messages.
     */
    private static final Pattern GREETING = Pattern.compile("HI, I AM " + CLIENT_REGEX);
    private static final Pattern BYE = Pattern.compile("BYE MATE!");
    private static final Pattern ADD_NODE = Pattern.compile("ADD NODE " + NAME_REGEX);
    private static final Pattern REMOVE_NODE = Pattern.compile("REMOVE NODE " + NAME_REGEX);
    private static final Pattern ADD_EDGE = Pattern.compile("ADD EDGE " + NAME_REGEX + " " + NAME_REGEX + " ([0-9]+)");
    private static final Pattern REMOVE_EDGE = Pattern.compile("REMOVE EDGE " + NAME_REGEX + " " + NAME_REGEX);
    private static final Pattern SHORTEST_PATH = Pattern.compile("SHORTEST PATH " + NAME_REGEX + " " + NAME_REGEX);
    private static final Pattern CLOSER_THAN = Pattern.compile("CLOSER THAN " + NUMERIC_REGEX + " " + NAME_REGEX);

    /**
     * Flag for an invalid path obtained as a result to the
     * Shortest Path call.
     */
    public static final Integer INVALID_PATH = -1;

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
            reply(String.format(Messages.GREETING.message(), this.sessionId));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if (Matcher.match(GREETING, inputLine) || Matcher.match(BYE, inputLine)) {
                    Pair<Boolean, String> greetingResult = processGreetingMessage(inputLine);
                    if (greetingResult.getKey()) {
                        reply(greetingResult.getValue());
                    } else {
                        break;
                    }
                } else {
                    reply(processGraphMessage(inputLine));
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
            reply(String.format(Messages.FAREWELL.message(), this.clientId, total));

            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            log.error("An error occurred while finishing socket for Session [{}].", this.sessionId, e);
        }
    }

    /**
     * Method responsible for processing a greeting message.
     * A greeting message is a message that starts with HI or BYE.
     *
     * @param inputLine String with the incoming message.
     * @return Pair object where the key is a flag that determines
     *         if the conversation can proceed (true if the conversation
     *         will proceed, false otherwise). If it will proceed, the
     *         Pair value will keep the message to be sent back.
     */
    public Pair<Boolean, String> processGreetingMessage(String inputLine) {
        String result = null;
        boolean continueConversation = true;
        if (Matcher.match(GREETING, inputLine)) {
            String[] tokens = tokenizeInput(inputLine);
            this.clientId = tokens[3];
            log.info("Session [{}] received greeting from Client [{}].", this.sessionId, this.clientId);
            result = String.format(Messages.GREETING_REPLY.message(), this.clientId);
        } else if (Matcher.match(BYE, inputLine)) {
            continueConversation = false;
        } else {
            log.warn("Session received a message that could not be recognized. Message was: [{}].", inputLine);
            result = Messages.SORRY.message();
        }
        return  new Pair<>(continueConversation, result);
    }

    /**
     * Method responsible for processing a graph message.
     * A graph message is a message that tells the program
     * to performa changes or queries on the graph.
     *
     * @param inputLine String with the incoming message.
     * @return Message with the result of the operation.
     */
    public String processGraphMessage(String inputLine) {
        String result = null;
        if (Matcher.match(ADD_NODE, inputLine)) {
            result = addNode(inputLine);
        } else if (Matcher.match(ADD_EDGE, inputLine)) {
            result = addEdge(inputLine);
        } else if (Matcher.match(REMOVE_NODE, inputLine)) {
            result = removeNode(inputLine);
        } else if (Matcher.match(REMOVE_EDGE, inputLine)) {
            result = removeEdge(inputLine);
        } else if (Matcher.match(SHORTEST_PATH, inputLine)) {
            result = shortestPath(inputLine);
        } else if (Matcher.match(CLOSER_THAN, inputLine)) {
            result = closerThan(inputLine);
        }
        return result;
    }

    /**
     * Parse the message that tells the application
     * to add a node and pass it to the graph.
     *
     * @param inputLine Instruction for adding the node.
     * @return Message with the result of the operation.
     */
    private String addNode(String inputLine) {
        String result;
        String[] tokens = tokenizeInput(inputLine);
        String name = tokens[2];
        log.info("Adding Node with name [{}].", name);
        if (graph.addNode(name)) {
            result = Messages.NODE_ADDED.message();
        } else {
            log.warn("Could not add Node with name [{}] because it already exists.", name);
            result = Messages.NODE_EXISTS.message();
        }
        return result;
    }

    /**
     * Parse the message that tells the application
     * to add an edge and pass it to the graph.
     *
     * @param inputLine Instruction for adding the edge.
     * @return Message with the result of the operation.
     */
    private String addEdge(String inputLine) {
        String result;
        String[] tokens = tokenizeInput(inputLine);
        String from = tokens[2], to = tokens[3];
        int weight = Integer.parseInt(tokens[4]);
        log.info("Adding Edge from Node [{}] to Node [{}] with Weight [{}].", from, to, weight);
        if (graph.addEdge(from, to, weight)) {
            result = Messages.EDGE_ADDED.message();
        } else {
            log.warn("Could not add Edge from Node [{}] to Node [{}] with Weight [{}] because one of " +
                    "the Nodes do not exists.", from, to, weight);
            result = Messages.NODE_NOT_FOUND.message();
        }
        return result;
    }

    /**
     * Parse the message that tells the application
     * to remove the edge and pass it to the graph.
     *
     * @param inputLine Instruction for removing the edge.
     * @return Message with the result of the operation.
     */
    private String removeEdge(String inputLine) {
        String result;
        String[] tokens = tokenizeInput(inputLine);
        String from = tokens[2], to = tokens[3];
        log.info("Removing Edge from Node [{}] to Node [{}].", from, to);
        if (graph.removeEdge(from, tokens[3])) {
            result = Messages.EDGE_REMOVED.message();
        } else {
            log.warn("Could not remove Edge from Node [{}] to Node [{}] because one of " +
                    "the Nodes do not exists.", from, to);
            result = Messages.NODE_NOT_FOUND.message();
        }
        return result;
    }

    /**
     * Parse the message that tells the application
     * to remove a node and pass it to the graph.
     *
     * @param inputLine Instruction for removing the node.
     * @return Message with the result of the operation.
     */
    private String removeNode(String inputLine) {
        String result;
        String[] tokens = tokenizeInput(inputLine);
        String name = tokens[2];
        log.info("Removing Node with name [{}].", name);
        if (graph.removeNode(name)) {
            result = Messages.NODE_REMOVED.message();
        } else {
            log.warn("Could not remove Node with name [{}] because it do not exists.", name);
            result = Messages.NODE_NOT_FOUND.message();
        }
        return result;
    }

    /**
     * Parse the message that tells the application
     * to get the shortest path between nodes.
     *
     * @param inputLine Instruction for getting the shortest path.
     * @return Message with the result of the operation.
     */
    private String shortestPath(String inputLine) {
        String result;
        String[] tokens = tokenizeInput(inputLine);
        String from = tokens[2], to = tokens[3];
        log.info("Getting Shortest Path from Node [{}] to Node [{}].", from, to);
        Integer path = graph.shortestPath(from, to);
        if (INVALID_PATH.equals(path)) {
            log.warn("Could not get Shortest Path from Node [{}] to Node [{}] because one of " +
                    "the Nodes does not exists.", from, to);
            result = Messages.NODE_NOT_FOUND.message();
        } else {
            log.info("Shortest Path from Node [{}] to Node [{}] is [{}].", from, to, path);
            result = path.toString();
        }
        return result;
    }

    /**
     * Parse the message that tells the application
     * to get closest nodes from a specific one with
     * the provided distance.
     *
     * @param inputLine Instruction for getting the closest nodes.
     * @return Message with the result of the operation.
     */
    private String closerThan(String inputLine) {
        String result;
        String[] tokens = tokenizeInput(inputLine);
        String weight = tokens[2], to = tokens[3];
        log.info("Getting Nodes Closer Than [{}] to Node [{}].", weight, to);
        List<String> closer = graph.closerThan(Integer.parseInt(weight), to);
        if (Objects.isNull(closer)) {
            log.warn("Could not get Nodes Closer Than [{}] to Node [{}] because this Node does not exists.",
                    weight, to);
            result = Messages.NODE_NOT_FOUND.message();
        } else if (closer.isEmpty()) {
            log.warn("Could not get Nodes Closer Than [{}] to Node [{}].", weight, to);
            result = "";
        } else{
            result = String.join(",", closer);
        }
        return result;
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

    /**
     * Reply to the Client using the {@link PrintWriter}.
     *
     * @param message Message to be sent.
     */
    private void reply(String message) {
        out.println(message);
    }

    /**
     * Inner class for encapsulating the Pattern Matching behavior.
     */
    private static final class Matcher {

        /**
         * Matches an input with provided {@link Pattern}.
         *
         * @param pattern Pattern for matching.
         * @param input Input to be validated.
         * @return true if it matches, false otherwise.
         */
        public static boolean match(Pattern pattern, String input) {
            java.util.regex.Matcher m = pattern.matcher(input);
            return m.matches();
        }

    }

}
