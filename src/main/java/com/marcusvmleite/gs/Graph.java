package com.marcusvmleite.gs;

import java.util.*;

public class Graph {

    private Map<String, Node> nodes;
    private Set<Edge> edges;

    public Graph() {
        this.nodes = new HashMap<>();
        this.edges = new HashSet<>();
    }

    public boolean addNode(String name) {
        boolean result = false;
        if (!nodes.containsKey(name)) {
            nodes.put(name, new Node(name));
            result = true;
        }
        return result;
    }

    public boolean addEdge(String from, String to, int weight) {
        boolean result = true;
        Node nodeFrom = nodes.get(from);
        Node nodeTo = nodes.get(to);
        if (Objects.isNull(nodeFrom) || Objects.isNull(nodeTo)) {
            result = false;
        } else {
            Edge edge = new Edge.Builder().from(nodeFrom).to(nodeTo).withWeight(weight).build();
            if (!edges.contains(edge)) {
                nodeFrom.addEdge(edge);
                edges.add(edge);
            }
        }
        return result;
    }

    public boolean removeNode(String name) {
        boolean result = false;
        if (nodes.containsKey(name)) {
            Node removed = nodes.remove(name);
            for (Edge edge : removed.edges) {
                edges.remove(edge);
            }
            result = true;
        }
        return result;
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
