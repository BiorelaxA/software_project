package com.example.graphapp.algo;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.List;

/**
 * 基于 Dijkstra 算法的最短路径实现
 */
public class DijkstraShortestPathService implements ShortestPathFinder {
    @Override
    public List<DefaultWeightedEdge> findPath(Graph<String, DefaultWeightedEdge> graph,
            String src,
            String dst) {
        return DijkstraShortestPath.findPathBetween(graph, src, dst)
                .getEdgeList();
    }
}