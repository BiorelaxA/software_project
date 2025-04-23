// file: src/main/java/com/example/graphapp/algo/PageRankService.java
package com.example.graphapp.algo;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import java.util.*;

/**
 * PageRank 算法实现
 */
public class PageRankService {
    /**
     * 计算 PageRank
     * 
     * @param graph         有向加权图
     * @param dampingFactor 阻尼系数 d
     * @param maxIterations 最大迭代次数
     * @param tolerance     收敛阈值
     * @return 节点到 PR 值的映射
     */
    public Map<String, Double> computePageRank(Graph<String, DefaultWeightedEdge> graph,
            double dampingFactor,
            int maxIterations,
            double tolerance) {
        int N = graph.vertexSet().size();
        double init = 1.0 / N;
        Map<String, Double> pr = new HashMap<>();
        Map<String, Double> newPr = new HashMap<>();
        for (String v : graph.vertexSet()) {
            pr.put(v, init);
        }
        for (int iter = 0; iter < maxIterations; iter++) {
            double maxDiff = 0;
            for (String v : graph.vertexSet()) {
                double sum = 0;
                for (DefaultWeightedEdge inE : graph.incomingEdgesOf(v)) {
                    String u = graph.getEdgeSource(inE);
                    double weightUV = graph.getEdgeWeight(inE);
                    double totalOut = 0;
                    for (DefaultWeightedEdge outE : graph.outgoingEdgesOf(u)) {
                        totalOut += graph.getEdgeWeight(outE);
                    }
                    if (totalOut > 0) {
                        sum += (weightUV / totalOut) * pr.get(u);
                    }
                }
                double value = (1 - dampingFactor) / N + dampingFactor * sum;
                newPr.put(v, value);
                maxDiff = Math.max(maxDiff, Math.abs(value - pr.get(v)));
            }
            pr.putAll(newPr);
            if (maxDiff < tolerance)
                break;
        }
        return pr;
    }
}