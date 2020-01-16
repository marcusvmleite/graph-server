package com.marcusvmleite.gs;

public class ObjectUtil {

    public static Graph getClearGraph() {
        Graph graph = Graph.getInstance();
        graph.clear();
        return graph;
    }

}
