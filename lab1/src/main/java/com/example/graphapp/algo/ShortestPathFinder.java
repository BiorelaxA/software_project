// file: src/main/java/com/example/graphapp/algo/ShortestPathFinder.java
package com.example.graphapp.algo;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.List;

/**
 * 最短路径接口：给定源和目标，返回边的列表
 */
public interface ShortestPathFinder {
    List<DefaultWeightedEdge> findPath(Graph<String, DefaultWeightedEdge> graph,
            String src,
            String dst);
}
