package com.marcusvmleite.gs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 *        calculated with Dijkstra Algorithm.
 *
 * All public methods are using ReentrantReadWriteLock to provide
 * Thread-Safety for Read and Write operations on the Graph.
 *
 * @author marcusvmleite
 * @since 13.01.2020
 * @version 1.0
 */
public class Graph {

    /**
     * Read and Write lock. Fair mode enabled.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

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
    public static Graph getInstance() {
        return INSTANCE;
    }

    /**
     * Method responsible for adding a {@link Node} to the Graph.
     * The Node will be added only if it does not exists.
     * This check is done by its name, so the Graph must have
     * only one Node with a specific name.
     *
     * @param name Name of the {@link Node} being added.
     * @return true if successfully added, false otherwise.
     */
    public boolean addNode(String name) {
        lock.writeLock().lock();
        try {
            boolean result = false;
            if (!this.nodes.containsKey(name)) {
                this.nodes.put(name, new Node(name));
                result = true;
            }
            return result;
        } finally {
            lock.writeLock().unlock();
        }
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
     * @param from From Node name.
     * @param to To Node name.
     * @param weight Weight of the Edge being added.
     * @return true if successfully added, false otherwise.
     */
    public boolean addEdge(String from, String to, int weight) {
        lock.writeLock().lock();
        try {
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
                } else {
                    Edge existingEdge = edges.get(edge);
                    if (weight < existingEdge.weight) {
                        existingEdge.weight = weight;
                    }
                }
            }
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Method responsible for removing a {@link Node}
     * from the Graph. It will check for the Node's existence.
     * It will check for existent Edges connected to this
     * Node and remove them accordingly.
     *
     * @param name Name of the Node being removed.
     * @return true if successfully added, false otherwise.
     */
    public boolean removeNode(String name) {
        lock.writeLock().lock();
        try {
            boolean result = false;
            if (this.nodes.containsKey(name)) {
                Node removed = this.nodes.remove(name);
                for (Edge edge : removed.edges) {
                    removeEdgeFromEdgeMap(edge);
                    removeEdgeFromNodeMap(edge.to, edge.from);
                }
                result = true;
            }
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Method responsible for removing an {@link Edge}
     * from the Graph. It will check for the Nodes' existence.
     *
     * @param from From Node of the Edge.
     * @param to To Node of the Edge.
     * @return true if successfully added, false otherwise.
     */
    public boolean removeEdge(String from, String to) {
        lock.writeLock().lock();
        try {
            boolean result = true;
            Node nodeFrom = this.nodes.get(from);
            Node nodeTo = this.nodes.get(to);
            if (Objects.isNull(nodeFrom) || Objects.isNull(nodeTo)) {
                result = false;
            } else {
                removeEdgeFromNodeMap(nodeFrom, nodeTo);
                removeEdgeFromEdgeMap(nodeFrom, nodeTo);
            }
            return result;
        } finally {
            lock.writeLock().unlock();
        }
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
    public Integer shortestPath(String from, String to) {
        lock.readLock().lock();
        try {
            Node nodeFrom = this.nodes.get(from);
            Node nodeTo = this.nodes.get(to);
            if (Objects.isNull(nodeFrom) || Objects.isNull(nodeTo)) {
                return -1;
            }
            Map<Node, Integer> distances = performDijkstra(nodeFrom);
            Integer result = distances.get(nodeTo);
            return result != null ? result : Integer.MAX_VALUE;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Method responsible for obtaining the nodes that are closer to
     * a provided Node than the given weight. It will check for the
     * Nodes's existence.
     *
     * This method uses Dijkstra's Algorithm for calculating
     * the distance between a Node and all other Nodes of the Graph.
     *
     * @param weight Provided weight.
     * @param to Target node for calculation.
     * @return List os Nodes's names closer to the provided Node.
     */
    public List<String> closerThan(int weight, String to) {
        lock.readLock().lock();
        try {
            Node nodeTo = this.nodes.get(to);
            if (Objects.isNull(nodeTo)) {
                return null;
            }
            List<String> result = new ArrayList<>();
            Map<Node, Integer> distances = performDijkstra(nodeTo);
            for (Map.Entry<Node, Integer> pair : distances.entrySet()) {
                Node curr = pair.getKey();
                if (!curr.equals(nodeTo) && pair.getValue() < weight) {
                    result.add(curr.name);
                }
            }
            Collections.sort(result);
            return result;
        } finally {
            lock.readLock().unlock();
        }
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
    static final class Node {

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
