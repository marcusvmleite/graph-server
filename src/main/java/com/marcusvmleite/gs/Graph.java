package com.marcusvmleite.gs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Graph {

    private Map<String, Node> nodes;
    private Map<Edge, Edge> edges;

    public Graph() {
        this.nodes = new ConcurrentHashMap<>();
        this.edges = new ConcurrentHashMap<>();
    }

    public synchronized boolean addNode(String name) {
        boolean result = false;
        if (!nodes.containsKey(name)) {
            nodes.put(name, new Node(name));
            result = true;
        }
        return result;
    }

    public synchronized boolean addEdge(String from, String to, int weight) {
        boolean result = true;
        Node nodeFrom = nodes.get(from);
        Node nodeTo = nodes.get(to);
        if (Objects.isNull(nodeFrom) || Objects.isNull(nodeTo)) {
            result = false;
        } else {
            Edge edge = new Edge.Builder().from(nodeFrom).to(nodeTo).withWeight(weight).build();
            if (!edges.containsKey(edge)) {
                nodeFrom.addEdge(edge);
                edges.put(edge, edge);
            } else {
                nodeFrom.addEdge(edges.get(edge));
            }
        }
        return result;
    }

    public synchronized boolean removeNode(String name) {
        boolean result = false;
        if (nodes.containsKey(name)) {
            Node removed = nodes.remove(name);
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
        Node nodeFrom = nodes.get(from);
        Node nodeTo = nodes.get(to);
        if (Objects.isNull(nodeFrom) || Objects.isNull(nodeTo)) {
            result = false;
        } else {
            removeEdgeFromNodeMap(nodeFrom, nodeTo);
            removeEdgeFromEdgeMap(nodeFrom, nodeTo);
        }
        return result;
    }

    public synchronized Integer shortestPath(String from, String to) {
        Node nodeFrom = nodes.get(from);
        Node nodeTo = nodes.get(to);
        if (Objects.isNull(nodeFrom) || Objects.isNull(nodeTo)) {
            return -1;
        }
        Map<Node, Integer> distances = performDijkstra(nodeFrom);
        Integer result = distances.get(nodeTo);
        return result != null ? result : Integer.MAX_VALUE;
    }

    public synchronized List<String> closerThan(int weight, String to) {
        Node nodeTo = nodes.get(to);
        if (Objects.isNull(nodeTo)) {
            return Collections.emptyList();
        }
        Map<Node, Integer> distances = performDijkstra(nodeTo);
        List<String> result = new ArrayList<>();
        for (Map.Entry<Node, Integer> pair : distances.entrySet()) {
            if (pair.getValue() < weight) {
                result.add(pair.getKey().name);
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
        for (Map.Entry<String, Node> pair : nodes.entrySet()) {
            if (pair.getValue().equals(nodeFrom)) {
                pair.getValue().edges.removeIf(curr -> curr.to.equals(nodeTo));
            }
        }
    }

    private Map<Node, Integer> performDijkstra(Node from) {

        Map<Node, Integer> distances = new HashMap<>();
        Queue<Node> q = new ArrayDeque<>(nodes.size());
        q.offer(from);
        Node curr;

        while (!q.isEmpty()) {
            curr = q.poll();
            for (Edge edge : curr.edges) {
                q.offer(edge.to);
                if (!distances.containsKey(edge.to) ||
                        distances.get(edge.to) > edge.weight + distances.get(curr)) {
                    distances.put(edge.to, edge.weight + distances.get(curr));
                }
            }
        }

        return distances;
    }

    static final class Node {

        String name;
        List<Graph.Edge> edges;

        Node(String name) {
            this.name = name;
            this.edges = new ArrayList<>();
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
