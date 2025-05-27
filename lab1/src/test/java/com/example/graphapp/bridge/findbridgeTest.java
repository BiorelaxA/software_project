package com.example.graphapp.bridge;

import java.util.List;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FindBridgeTest {
    private Graph<String, DefaultWeightedEdge> graph;
    private FindBridge bridgeFinder;

    @BeforeEach
    void setUp() {
        graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        bridgeFinder = new FindBridge();
    }

    @Test
    void tc1_singleBridge() {
        graph.addVertex("a");
        graph.addVertex("b");
        graph.addVertex("c");
        graph.addEdge("a", "b");
        graph.addEdge("b", "c");
        assertEquals(List.of("b"), bridgeFinder.findBridgeWords(graph, "a", "c"));
    }

    @Test
    void tc2_multipleBridges() {
        graph.addVertex("a");
        graph.addVertex("b");
        graph.addVertex("c");
        graph.addVertex("d");
        graph.addEdge("a", "b");
        graph.addEdge("b", "c");
        graph.addEdge("a", "d");
        graph.addEdge("d", "c");
        List<String> result = bridgeFinder.findBridgeWords(graph, "a", "c");
        assertTrue(result.containsAll(List.of("b", "d")) && result.size() == 2);
    }

    @Test
    void tc3_noBridge() {
        graph.addVertex("a");
        graph.addVertex("b");
        graph.addVertex("c");
        graph.addEdge("a", "b");
        graph.addEdge("b", "c");
        assertTrue(bridgeFinder.findBridgeWords(graph, "b", "a").isEmpty());
    }

    @Test
    void tc4_startMissing() {
        graph.addVertex("a");
        graph.addVertex("b");
        graph.addVertex("c");
        graph.addEdge("a", "b");
        graph.addEdge("b", "c");
        assertTrue(bridgeFinder.findBridgeWords(graph, "x", "c").isEmpty());
    }

    @Test
    void tc5_endMissing() {
        graph.addVertex("a");
        graph.addVertex("b");
        graph.addVertex("c");
        graph.addEdge("a", "b");
        graph.addEdge("b", "c");
        assertTrue(bridgeFinder.findBridgeWords(graph, "a", "y").isEmpty());
    }

    @Test
    void tc6_bothMissing() {
        assertTrue(bridgeFinder.findBridgeWords(graph, "x", "y").isEmpty());
    }

    @Test
    void tc7_emptyGraph() {
        assertTrue(bridgeFinder.findBridgeWords(graph, "a", "b").isEmpty());
    }

    @Test
    void tc8_selfLoop() {
        graph.addVertex("a");
        graph.addEdge("a", "a");
        assertEquals(List.of("a"), bridgeFinder.findBridgeWords(graph, "a", "a"));
    }
}