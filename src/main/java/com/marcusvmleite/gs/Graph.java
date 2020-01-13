package com.marcusvmleite.gs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Graph {

    private static final Graph INSTANCE = new Graph();

    private Map<String, Node> nodes;
    private Map<Edge, Edge> edges;

    private boolean mustRecalculateFloydWarshall = true;
    double[][] distances;

    private Graph() {
        this.nodes = new ConcurrentHashMap<>();
        this.edges = new ConcurrentHashMap<>();
    }

    public synchronized static Graph getInstance() {
        return INSTANCE;
    }

    public synchronized boolean addNode(String name) {
        boolean result = false;
        if (!this.nodes.containsKey(name)) {
            this.nodes.put(name, new Node(name));
            result = true;
            this.mustRecalculateFloydWarshall = true;
        }
        return result;
    }

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

    public synchronized List<String> closerThan(int weight, String to) {
        Node nodeTo = this.nodes.get(to);
        if (Objects.isNull(nodeTo)) {
            return null;
        }
        if (this.mustRecalculateFloydWarshall) {
            this.distances = performFloydWarshall();

            //We'll recalculate Floyd-Warshall only if the Graph change.
            this.mustRecalculateFloydWarshall = false;
        }
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Node> pair : nodes.entrySet()) {
            Node node = pair.getValue();
            if (!node.equals(nodeTo) && this.distances[nodeTo.idx][node.idx] < weight) {
                result.add(node.name);
            }
        }
        Collections.sort(result);
        return result;
    }

    private void removeEdgeFromEdgeMap(Edge edge) {
        removeEdgeFromEdgeMap(edge.from, edge.to);
    }

    private void removeEdgeFromEdgeMap(Node nodeFrom, Node nodeTo) {
        edges.entrySet().removeIf(e -> e.getKey().from.equals(nodeFrom) && e.getKey().to.equals(nodeTo));
    }

    private void removeEdgeFromNodeMap(Node nodeFrom, Node nodeTo) {
        for (Map.Entry<String, Node> pair : this.nodes.entrySet()) {
            if (pair.getValue().equals(nodeFrom)) {
                pair.getValue().edges.removeIf(curr -> curr.to.equals(nodeTo));
            }
        }
    }

    private Map<Node, Integer> performDijkstra(Node from) {

        Map<Node, Integer> distances = new HashMap<>();
        distances.put(from, 0);
        Queue<Node> q = new ArrayDeque<>(this.nodes.size());
        q.offer(from);
        Node curr;

        Set<Node> visited = new HashSet<>();

        while (!q.isEmpty()) {
            curr = q.poll();
            for (Edge edge : curr.edges) {
                if (!visited.contains(edge.to)) {
                    if (!distances.containsKey(edge.to) ||
                            distances.get(edge.to) > edge.weight + distances.get(curr) ||
                            distances.get(edge.to) == 0) {
                        distances.put(edge.to, edge.weight + distances.get(curr));
                        q.offer(edge.to);
                    }
                }
            }
            visited.add(curr);
        }

        return distances;
    }

    private double[][] performFloydWarshall() {
        double[][] distances = new double[nodes.size()][nodes.size()];
        for (double[] row : distances) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        int count = 0;
        for (Map.Entry<String, Node> pair : this.nodes.entrySet()) {
            Node node = pair.getValue();
            node.idx = count++;
        }
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

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public Map<Edge, Edge> getEdges() {
        return edges;
    }

    static final class Node {

        int idx;
        String name;
        Set<Graph.Edge> edges;

        Node(String name) {
            this.name = name;
            this.edges = new TreeSet<>();
        }

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

    private static final class Edge implements Comparable<Edge> {

        Node from;
        Node to;
        int weight;

        Edge(Builder builder) {
            this.from = builder.from;
            this.to = builder.to;
            this.weight = builder.weight;
        }

        @Override
        public int compareTo(Edge o) {
            return this.weight - o.weight;
        }

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
