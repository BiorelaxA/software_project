package com.example.graphapp.bridge;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

public class findbridge {
    public List<String> findBridgeWords(Graph<String, DefaultWeightedEdge> graph, String word1, String word2) {
        List<String> bridgeWords = new ArrayList<>();

        if (!graph.containsVertex(word1) || !graph.containsVertex(word2)) {
            System.out.println("No " + word1 + " or " + word2 + " in the graph!");
            return bridgeWords;
        }

        for (DefaultWeightedEdge edgeFromWord1 : graph.outgoingEdgesOf(word1)) {
            String intermediate = graph.getEdgeTarget(edgeFromWord1);
            if (graph.containsEdge(intermediate, word2)) {
                bridgeWords.add(intermediate);
            }
        }

        if (bridgeWords.isEmpty()) {
            System.out.println("No bridge words from " + word1 + " to " + word2 + "!");
        } else {
            System.out.print("The bridge words from " + word1 + " to " + word2 + " are: ");
            for (int i = 0; i < bridgeWords.size(); i++) {
                System.out.print(bridgeWords.get(i));
                if (i < bridgeWords.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println(".");
        }

        return bridgeWords;
    }

}
