package com.marcusvmleite.gs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Graph {

    private Map<String, Node> nodes;
    private Map<Integer, Node> nodesIdx;
    private Map<Edge, Edge> edges;

    public Graph() {
        this.nodes = new ConcurrentHashMap<>();
        this.nodesIdx = new ConcurrentHashMap<>();
        this.edges = new ConcurrentHashMap<>();
    }

    public synchronized boolean addNode(String name) {
        boolean result = false;
        if (!this.nodes.containsKey(name)) {
            this.nodes.put(name, new Node(name));
            result = true;
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
            } else {
                nodeFrom.addEdge(this.edges.get(edge));
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
            return Collections.emptyList();
        }
        double[][] distances = performFloydWarshall();
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Node> pair : nodes.entrySet()) {
            Node node = pair.getValue();
            if (distances[node.idx][nodeTo.idx] < weight) {
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
            if (visited.contains(curr)) {
                continue;
            }
            visited.add(curr);
            for (Edge edge : curr.edges) {
                q.offer(edge.to);
                if (!distances.containsKey(edge.to) ||
                        distances.get(edge.to) > edge.weight + distances.get(curr) ||
                        distances.get(edge.to) == 0) {
                    distances.put(edge.to, edge.weight + distances.get(curr));
                }
            }
        }

        return distances;
    }

    private double[][] performFloydWarshall() {
        nodesIdx.clear();
        double[][] distances = new double[nodes.size()][nodes.size()];
        for (double[] row : distances) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        int count = 0;
        for (Map.Entry<String, Node> pair : this.nodes.entrySet()) {
            Node node = pair.getValue();
            node.idx = count;
            nodesIdx.put(node.idx, node);
            ++count;
        }
        for (Map.Entry<String, Node> pair : this.nodes.entrySet()) {
            Node node = pair.getValue();
            for (Edge edge : node.edges) {
                distances[node.idx][edge.to.idx] = edge.weight;
            }
        }
        for (int k = 0; k < nodes.size(); k++) {
            // Pick all vertices as source one by one
            for (int i = 0; i < nodes.size(); i++) {
                // Pick all vertices as destination for the
                // above picked source
                for (int j = 0; j < nodes.size(); j++) {
                    // If vertex k is on the shortest path from
                    // i to j, then update the value of dist[i][j]
                    if (distances[i][k] + distances[k][j] < distances[i][j])
                        distances[i][j] = distances[i][k] + distances[k][j];
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
            this.edges = new HashSet<>();
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

    private static final class Edge {

        Node from;
        Node to;
        int weight;

        Edge(Builder builder) {
            this.from = builder.from;
            this.to = builder.to;
            this.weight = builder.weight;
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
                    edge.to.equals(this.to) &&
                    edge.weight == this.weight;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + from.hashCode();
            result = 31 * result + to.hashCode();
            result = 31 * result + this.weight;
            return result;
        }

    }

}
