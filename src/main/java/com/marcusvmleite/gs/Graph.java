package com.marcusvmleite.gs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that represents a Directed Graph.
 * This class is a Singleton and will be shared by
 * all Client Sessions during application life-cycle.
 *
 * It provides operations to build the Graph:
 *      - Add and Remove a Node;
 *      - Add and Remove an Edge.
 * Besides, provides some common queries:
 *      - Shortest Path between two Nodes, calculated
 *        with Dijkstra Algorithm;
 *      - Closest Nodes from a specific Node within a distance,
 *        calculated with Floyd-Warshall Algorithm.
 *
 * All public methods are Synchronized to provide Thread-Safety
 * and Graph Consistency between changing structure operations
 * and queries.
 *
 * @author marcusvmleite
 * @since 13.01.2020
 * @version 1.0
 */
public class Graph {

    /**
     * Unique instance (Singleton Pattern).
     */
    private static final Graph INSTANCE = new Graph();

    /**
     * Map to keep all Nodes of the Graph.
     */
    private Map<String, Node> nodes;

    /**
     * Map to keep all Edges of the Graph.
     */
    private Map<Edge, Edge> edges;

    /**
     * Flag to control when do we need to (re)calculate
     * Floyd-Warshall. As this calculate Algorithm calculates
     * distance between all Nodes of the Graph, this is not a
     * very performatic operation (Big O for Floyd-Warshall is
     * O(N^3), where N is the number of Nodes.
     *
     * When we calculate it, we'll only calculate again
     * when there is change in the Graph's structure.
     */
    private boolean mustRecalculateFloydWarshall = true;

    /**
     * Floyd-Warshall calculated distances between all
     * Nodes in the Graph.
     */
    double[][] distancesFloydWarshall;

    /**
     * Class constructor that initializes Thread-Safe Maps.
     */
    private Graph() {
        this.nodes = new ConcurrentHashMap<>();
        this.edges = new ConcurrentHashMap<>();
    }

    /**
     * Method responsible for obtaining the single Instance
     * of this class.
     *
     * @return Graph's unique instance.
     */
    public synchronized static Graph getInstance() {
        return INSTANCE;
    }

    /**
     * Method responsible for adding a {@link Node} to the Graph.
     * The Node will be added only if it does not exists.
     * This check is done by its name, so the Graph must have
     * only one Node with a specific name.
     *
     * When we add a new node, we must tell the Graph
     * that next time we'll need to recalculate Floyd-Warshall.
     *
     * @param name Name of the {@link Node} being added.
     * @return true if successfully added, false otherwise.
     */
    public synchronized boolean addNode(String name) {
        boolean result = false;
        if (!this.nodes.containsKey(name)) {
            this.nodes.put(name, new Node(name));
            result = true;
            this.mustRecalculateFloydWarshall = true;
        }
        return result;
    }

    /**
     * Method responsible for adding an {@link Edge} to the Graph.
     * The method checks for the existence of the provided
     * Nodes by its names.
     *
     * If an Edge between provided Nodes already exists
     * with a biggest weight, the method will only update
     * the weight of the existent Edge.
     *
     * When we add a new Edge, we must tell the Graph
     * that next time we'll need to recalculate Floyd-Warshall.
     *
     * @param from From Node name.
     * @param to To Node name.
     * @param weight Weight of the Edge being added.
     * @return true if successfully added, false otherwise.
     */
    public synchronized boolean addEdge(String from, String to, int weight) {
        boolean result = true;
        Node nodeFrom = this.nodes.get(from);
        Node nodeTo = this.nodes.get(to);
        if (Objects.isNull(nodeFrom) || Objects.isNull(nodeTo)) {
            result = false;
        } else {
            Edge edge = new Edge.Builder().from(nodeFrom).to(nodeTo).withWeight(weight).build();
            if (!this.edges.containsKey(edge)) {
                nodeFrom.addEdge(edge);
                this.edges.put(edge, edge);
                this.mustRecalculateFloydWarshall = true;
            } else {
                Edge existingEdge = edges.get(edge);
                if (weight < existingEdge.weight) {
                    existingEdge.weight = weight;
                    this.mustRecalculateFloydWarshall = true;
                }
            }
        }
        return result;
    }

    /**
     * Method responsible for removing a {@link Node}
     * from the Graph. It will check for the Node's existence.
     * It will check for existent Edges connected to this
     * Node and remove them accordingly.
     *
     * When we remove a Node, we must tell the Graph
     * that next time we'll need to recalculate Floyd-Warshall.
     *
     * @param name Name of the Node being removed.
     * @return true if successfully added, false otherwise.
     */
    public synchronized boolean removeNode(String name) {
        boolean result = false;
        if (this.nodes.containsKey(name)) {
            Node removed = this.nodes.remove(name);
            for (Edge edge : removed.edges) {
                removeEdgeFromEdgeMap(edge);
                removeEdgeFromNodeMap(edge.to, edge.from);
            }
            result = true;
            this.mustRecalculateFloydWarshall = true;
        }
        return result;
    }

    /**
     * Method responsible for removing an {@link Edge}
     * from the Graph. It will check for the Nodes' existence.
     *
     * When we remove a Node, we must tell the Graph
     * that next time we'll need to recalculate Floyd-Warshall.
     *
     * @param from From Node of the Edge.
     * @param to To Node of the Edge.
     * @return true if successfully added, false otherwise.
     */
    public synchronized boolean removeEdge(String from, String to) {
        boolean result = true;
        Node nodeFrom = this.nodes.get(from);
        Node nodeTo = this.nodes.get(to);
        if (Objects.isNull(nodeFrom) || Objects.isNull(nodeTo)) {
            result = false;
        } else {
            removeEdgeFromNodeMap(nodeFrom, nodeTo);
            removeEdgeFromEdgeMap(nodeFrom, nodeTo);
            this.mustRecalculateFloydWarshall = true;
        }
        return result;
    }

    /**
     * Method responsible for obtaining the Shortest Distance
     * between two provided Nodes. It will check for the Nodes'
     * existence.
     *
     * This method uses Dijkstra's Algorithm for calculating
     * the distance between a Node and all other Nodes of the Graph.
     *
     * If the nodes are not connected, it will return Integer.MAX_VALUE.
     *
     * @param from From Node of the Edge.
     * @param to To Node of the Edge.
     * @return Calculated distance between the nodes, of type {@link Integer}
     *         If at least one of the Nodes does not exists, -1 will be returned.
     */
    public synchronized Integer shortestPath(String from, String to) {
        Node nodeFrom = this.nodes.get(from);
        Node nodeTo = this.nodes.get(to);
        if (Objects.isNull(nodeFrom) || Objects.isNull(nodeTo)) {
            return -1;
        }
        Map<Node, Integer> distances = performDijkstra(nodeFrom);
        Integer result = distances.get(nodeTo);
        return result != null ? result : Integer.MAX_VALUE;
    }

    /**
     * Method responsible for obtaining the nodes that are closer to
     * a provided Node than the given weight. It will check for the
     * Nodes's existence.
     *
     * This method uses Floyd-Warshall's Algorithm, that calculates
     * all distances between all Nodes in the Graph. When calculated,
     * it will be flagged that we do not need to recalculate it, unless
     * there is a change in the Graph's structure.
     *
     * @param weight Provided weight.
     * @param to Target node for calculation.
     * @return List os Nodes's names closer to the provided Node.
     */
    public synchronized List<String> closerThan(int weight, String to) {
        Node nodeTo = this.nodes.get(to);
        if (Objects.isNull(nodeTo)) {
            return null;
        }

        //Check if its needed to (re)calculate Floyd-Warshall,
        //otherwise it will be used previously calculated result.
        if (this.mustRecalculateFloydWarshall) {
            this.distancesFloydWarshall = performFloydWarshall();

            //We'll (re)calculate again Floyd-Warshall only if the Graph change.
            this.mustRecalculateFloydWarshall = false;
        }
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Node> pair : nodes.entrySet()) {
            Node node = pair.getValue();
            if (!node.equals(nodeTo) && this.distancesFloydWarshall[nodeTo.idx][node.idx] < weight) {
                result.add(node.name);
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Remove an {@link Edge} from the Edges' map.
     *
     * @param edge Provided Edge for removal.
     */
    private void removeEdgeFromEdgeMap(Edge edge) {
        removeEdgeFromEdgeMap(edge.from, edge.to);
    }

    /**
     * Remove an {@link Edge} from the Edges' map provided
     * the Nodes that compose it.
     *
     * @param nodeFrom From Node of the Edge.
     * @param nodeTo To Node of the Edge.
     */
    private void removeEdgeFromEdgeMap(Node nodeFrom, Node nodeTo) {
        edges.entrySet().removeIf(e -> e.getKey().from.equals(nodeFrom) && e.getKey().to.equals(nodeTo));
    }

    /**
     * Method responsible for scanning all the Nodes in the Nodes' map
     * and removing all occurrences of the Edge composed by the provided
     * Nodes.
     *
     * @param nodeFrom From Node of the Edge.
     * @param nodeTo To Node of the Edge.
     */
    private void removeEdgeFromNodeMap(Node nodeFrom, Node nodeTo) {
        for (Map.Entry<String, Node> pair : this.nodes.entrySet()) {
            if (pair.getValue().equals(nodeFrom)) {
                pair.getValue().edges.removeIf(curr -> curr.to.equals(nodeTo));
            }
        }
    }

    private Map<Node, Integer> performDijkstra(Node from) {

        Map<Node, Integer> distances = new HashMap<>();
        Set<Node> visited = new HashSet<>();

        distances.put(from, 0);
        from.weight = 0;

        PriorityQueue<Node> q = new PriorityQueue<>(Comparator.comparingInt(o -> o.weight));
        q.offer(from);
        Node curr;

        while (!q.isEmpty()) {
            curr = q.poll();
            if (visited.contains(curr)) {
                continue;
            }
            for (Edge edge : curr.edges) {
                if (!distances.containsKey(edge.to) ||
                        distances.get(edge.to) > edge.weight + distances.get(curr)) {
                    distances.put(edge.to, edge.weight + distances.get(curr));
                    edge.to.weight = edge.weight + distances.get(curr);
                    q.offer(edge.to);
                }
            }
            visited.add(curr);
        }
        return distances;
    }

    /**
     * Performs Floyd-Warshall Algorithm on the Graph.
     * Floyd-Warshall is a well known algorithm that calculates
     * the shortest distance between all Nodes in a directed and
     * weighted graph.
     *
     * @return Calculated distances stored in a 2D Matrix.
     */
    private double[][] performFloydWarshall() {
        double[][] distances = new double[nodes.size()][nodes.size()];
        for (double[] row : distances) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        //We give each Node a unique index for helping us
        //storing it in the 2D Matrix.
        int count = 0;
        for (Map.Entry<String, Node> pair : this.nodes.entrySet()) {
            Node node = pair.getValue();
            node.idx = count++;
        }
        //Then we initialize our distance matrix with the current
        //known weights from our edges.
        for (Map.Entry<String, Node> pair : this.nodes.entrySet()) {
            Node node = pair.getValue();
            for (Edge edge : node.edges) {
                distances[node.idx][edge.to.idx] = edge.weight;
            }
        }
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                for (int k = 0; k < nodes.size(); k++) {
                    if (distances[j][i] + distances[i][k] < distances[j][k])
                        distances[j][k] = distances[j][i] + distances[i][k];
                }
            }
        }
        return distances;
    }

    /**
     * Get the Nodes of the Graph.
     *
     * @return Map of Nodes.
     */
    public Map<String, Node> getNodes() {
        return nodes;
    }

    /**
     * Get the Edges of the Graph.
     *
     * @return Map of Edges.
     */
    public Map<Edge, Edge> getEdges() {
        return edges;
    }

    /**
     * Inner class that represents a Node in the Graph.
     * Attributes have accessibility default to make the
     * Graph code less verbose (and this class is used
     * only inside Graph).
     *
     * @author marcusvmleite
     * @since 13.01.2020
     * @version 1.0
     */
    private static final class Node {

        /**
         * Index that will be used to assemble the 2D Matrix
         * that is used to compute Floyd-Warshall Algorithm.
         */
        int idx;

        /**
         * Name of the Node.
         */
        String name;

        /**
         * All edges from this done.
         */
        Set<Graph.Edge> edges;

        /**
         * This redundant weight attribute will be used for the
         * PriorityQueue in Dijkstra Algorithm.
         */
        int weight;

        /**
         * Node's constructor.
         * Edges set is built with {@link TreeSet} to guarantee
         * sorting by the Edge's weight, needed for Dijkstra Algorithm.
         *
         * @param name Name of the Node.
         */
        Node(String name) {
            this.name = name;
            this.edges = new TreeSet<>();
        }

        /**
         * Add a {@link Edge} to the adjacency set.
         *
         * @param edge {@link Edge} to be added.
         */
        void addEdge(Edge edge) {
            this.edges.add(edge);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Node)) {
                return false;
            }
            Node node = (Node) o;
            return node.name.equals(this.name);
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + name.hashCode();
            return result;
        }

    }

    /**
     * Inner class that represents an Edge in the Graph.
     * Attributes have accessibility default to make the
     * Graph code less verbose (and this class is used
     * only inside Graph).
     *
     * @author marcusvmleite
     * @since 13.01.2020
     * @version 1.0
     */
    private static final class Edge implements Comparable<Edge> {

        /**
         * From {@link Node}.
         */
        Node from;

        /**
         * To {@link Node}.
         */
        Node to;

        /**
         * Weight of the Edge.
         */
        int weight;

        Edge(Builder builder) {
            this.from = builder.from;
            this.to = builder.to;
            this.weight = builder.weight;
        }

        /**
         * Builder Pattern.
         */
        public static final class Builder {

            private Node from;
            private Node to;
            private int weight;

            public Builder from(Node from) {
                this.from = from;
                return this;
            }

            public Builder to(Node to) {
                this.to = to;
                return this;
            }

            public Builder withWeight(int weight) {
                this.weight = weight;
                return this;
            }

            public Edge build() {
                return new Edge(this);
            }

        }

        @Override
        public int compareTo(Edge o) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if (this == o || this.equals(o)) {
                return EQUAL;
            }

            if (this.weight > o.weight) {
                return AFTER;
            } else if (this.weight < o.weight) {
                return BEFORE;
            }

            if (!this.from.name.equals(o.from.name)) {
                return this.from.name.compareTo(o.from.name);
            } else if (!this.to.name.equals(o.to.name)) {
                return this.to.name.compareTo(o.to.name);
            }

            return EQUAL;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Edge)) {
                return false;
            }
            Edge edge = (Edge) o;
            return edge.from.equals(this.from) &&
                    edge.to.equals(this.to);
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + from.hashCode();
            result = 31 * result + to.hashCode();
            return result;
        }

    }

}
