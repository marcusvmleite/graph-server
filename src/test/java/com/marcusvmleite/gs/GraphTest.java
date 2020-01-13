package com.marcusvmleite.gs;

import org.junit.Test;

import static org.junit.Assert.*;

public class GraphTest {

    private static final String NODE_TEST_1 = "NODE-TEST-1";
    private static final String NODE_TEST_2 = "NODE-TEST-2";
    private static final String NODE_TEST_3 = "NODE-TEST-3";
    private static final String UNKNOWN = "UNKNOWN";

    @Test
    public void testAddNode() {

        Graph graph = new Graph();

        assertTrue(graph.addNode(NODE_TEST_1));
        assertEquals(1, graph.getNodes().size());

        assertFalse(graph.addNode(NODE_TEST_1));
        assertEquals(1, graph.getNodes().size());

        assertTrue(graph.addNode(NODE_TEST_2));
        assertEquals(2, graph.getNodes().size());

        assertTrue(graph.addNode(NODE_TEST_3));
        assertEquals(3, graph.getNodes().size());
    }

    @Test
    public void testAddEdge() {

        Graph graph = new Graph();

        graph.addNode(NODE_TEST_1);
        graph.addNode(NODE_TEST_2);
        graph.addNode(NODE_TEST_3);

        assertTrue(graph.addEdge(NODE_TEST_1, NODE_TEST_2, 1));
        assertTrue(graph.addEdge(NODE_TEST_1, NODE_TEST_2, 1));
        assertTrue(graph.addEdge(NODE_TEST_1, NODE_TEST_2, 2));
        assertFalse(graph.addEdge("UNKNOWN", NODE_TEST_2, 1));

        assertEquals(2, graph.getEdges().size());

        assertEquals(2, graph.getNodes().get(NODE_TEST_1).edges.size());
    }

    @Test
    public void testRemoveNode() {

        Graph graph = new Graph();

        assertTrue(graph.addNode(NODE_TEST_1));
        assertEquals(1, graph.getNodes().size());

        assertTrue(graph.removeNode(NODE_TEST_1));
        assertEquals(0, graph.getNodes().size());

        assertTrue(graph.addNode(NODE_TEST_1));
        assertTrue(graph.addNode(NODE_TEST_2));
        assertEquals(2, graph.getNodes().size());
        assertTrue(graph.removeNode(NODE_TEST_1));
        assertEquals(1, graph.getNodes().size());
        assertTrue(graph.removeNode(NODE_TEST_2));
        assertEquals(0, graph.getNodes().size());
    }

    @Test
    public void testRemoveEdge() {
        Graph graph = new Graph();

        assertTrue(graph.addNode(NODE_TEST_1));
        assertEquals(1, graph.getNodes().size());
        assertTrue(graph.addNode(NODE_TEST_2));
        assertEquals(2, graph.getNodes().size());
        assertTrue(graph.addNode(NODE_TEST_3));
        assertEquals(3, graph.getNodes().size());

        assertTrue(graph.addEdge(NODE_TEST_1, NODE_TEST_2, 1));
        assertTrue(graph.addEdge(NODE_TEST_1, NODE_TEST_3, 2));
        assertTrue(graph.addEdge(NODE_TEST_2, NODE_TEST_3, 3));
        assertEquals(3, graph.getEdges().size());
        assertEquals(2, graph.getNodes().get(NODE_TEST_1).edges.size());
        assertEquals(1, graph.getNodes().get(NODE_TEST_2).edges.size());

        assertFalse(graph.removeEdge(UNKNOWN, NODE_TEST_3));
        assertTrue(graph.removeEdge(NODE_TEST_1, NODE_TEST_2));
        assertEquals(2, graph.getEdges().size());
        assertEquals(1, graph.getNodes().get(NODE_TEST_1).edges.size());
    }

    @Test
    public void testShortestPath() {
        Graph graph = new Graph();

        assertTrue(graph.addNode(NODE_TEST_1));
        assertEquals(1, graph.getNodes().size());
        assertTrue(graph.addNode(NODE_TEST_2));
        assertEquals(2, graph.getNodes().size());
        assertTrue(graph.addNode(NODE_TEST_3));
        assertEquals(3, graph.getNodes().size());

        assertTrue(graph.addEdge(NODE_TEST_1, NODE_TEST_2, 1));
        assertTrue(graph.addEdge(NODE_TEST_1, NODE_TEST_3, 2));
        assertTrue(graph.addEdge(NODE_TEST_2, NODE_TEST_3, 3));

        assertEquals(Integer.valueOf(1), graph.shortestPath(NODE_TEST_1, NODE_TEST_2));
        assertEquals(Integer.valueOf(2), graph.shortestPath(NODE_TEST_1, NODE_TEST_3));
        assertEquals(Integer.valueOf(3), graph.shortestPath(NODE_TEST_2, NODE_TEST_3));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), graph.shortestPath(NODE_TEST_3, NODE_TEST_1));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), graph.shortestPath(NODE_TEST_3, NODE_TEST_2));
    }

    @Test
    public void testCloserThan() {
        Graph graph = new Graph();

        assertTrue(graph.addNode(NODE_TEST_1));
        assertEquals(1, graph.getNodes().size());
        assertTrue(graph.addNode(NODE_TEST_2));
        assertEquals(2, graph.getNodes().size());
        assertTrue(graph.addNode(NODE_TEST_3));
        assertEquals(3, graph.getNodes().size());

        assertTrue(graph.addEdge(NODE_TEST_1, NODE_TEST_2, 1));
        assertTrue(graph.addEdge(NODE_TEST_1, NODE_TEST_3, 2));
        assertTrue(graph.addEdge(NODE_TEST_2, NODE_TEST_3, 5));

        assertTrue(graph.closerThan(2, UNKNOWN).isEmpty());
        assertEquals(2, graph.closerThan(10, NODE_TEST_3).size());
        assertEquals(1, graph.closerThan(5, NODE_TEST_3).size());
    }

}
