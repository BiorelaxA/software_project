// file: src/main/java/com/example/graphapp/input/GraphBuilder.java
package com.example.graphapp.input;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * 构建有向带权图：节点为单词，边的权重为相邻出现次数
 */
public class GraphBuilder {
    public Graph<String, DefaultWeightedEdge> buildGraph(File file) throws IOException {
        Graph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] words = line.trim().toLowerCase().split("\\s+");
                for (int i = 0; i < words.length - 1; i++) {
                    String a = words[i];
                    String b = words[i + 1];
                    graph.addVertex(a);
                    graph.addVertex(b);
                    DefaultWeightedEdge edge = graph.getEdge(a, b);
                    if (edge == null) {
                        edge = graph.addEdge(a, b);
                        graph.setEdgeWeight(edge, 1);
                    } else {
                        graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + 1);
                    }
                }
            }
        }
        return graph;
    }
}
