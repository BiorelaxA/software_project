package com.example.graphapp.ui;

import com.example.graphapp.input.GraphBuilder;
import com.example.graphapp.algo.ShortestPathFinder;
import com.example.graphapp.algo.DijkstraShortestPathService;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
// import com.mxgraph.util.mxCellHighlight;

/**
 * 主界面：加载文件、渲染图、展示最短路径
 */
public class MainFrame extends JFrame {
    private mxGraph graphUI = new mxGraph();
    private mxGraphComponent graphComponent = new mxGraphComponent(graphUI);

    private Graph<String, DefaultWeightedEdge> graphModel;
    private Map<DefaultWeightedEdge, Object> edgeToCellMap = new HashMap<>();

    public MainFrame() {
        super("GraphVisualizer");
        JButton loadBtn = new JButton("加载文件");
        JButton pathBtn = new JButton("最短路径");
        JTextField srcField = new JTextField(10);
        JTextField dstField = new JTextField(10);

        JPanel panel = new JPanel();
        panel.add(loadBtn);
        panel.add(new JLabel("源:"));
        panel.add(srcField);
        panel.add(new JLabel("终:"));
        panel.add(dstField);
        panel.add(pathBtn);

        loadBtn.addActionListener(e -> chooseAndLoad());
        pathBtn.addActionListener(e -> showShortestPath(
                srcField.getText().trim().toLowerCase(),
                dstField.getText().trim().toLowerCase()));

        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(graphComponent, BorderLayout.CENTER);
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void chooseAndLoad() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                graphModel = new GraphBuilder().buildGraph(file);
                renderGraph(graphModel);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "读取文件失败：" + ex.getMessage());
            }
        }
    }

    private void renderGraph(Graph<String, DefaultWeightedEdge> g) {
        Object parent = graphUI.getDefaultParent();
        graphUI.getModel().beginUpdate();
        try {
            graphUI.getModel().setVisible(g, false);
            edgeToCellMap.clear();
            Map<String, Object> vMap = new HashMap<>();
            int x = 20, y = 20;
            for (String v : g.vertexSet()) {
                Object cell = graphUI.insertVertex(parent, null, v, x, y, 80, 30);
                vMap.put(v, cell);
                x += 100;
                if (x > 700) {
                    x = 20;
                    y += 70;
                }
            }
            for (DefaultWeightedEdge e : g.edgeSet()) {
                String src = g.getEdgeSource(e);
                String dst = g.getEdgeTarget(e);
                Object cell = graphUI.insertEdge(parent, null,
                        (int) g.getEdgeWeight(e), vMap.get(src), vMap.get(dst));
                edgeToCellMap.put(e, cell);
            }
        } finally {
            graphUI.getModel().endUpdate();
        }
    }

    private void showShortestPath(String src, String dst) {
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }

        // 还原所有边的样式为默认
        for (Object cell : edgeToCellMap.values()) {
            graphUI.setCellStyle("strokeColor=black", new Object[] { cell });
        }

        ShortestPathFinder finder = new DijkstraShortestPathService();
        List<DefaultWeightedEdge> pathEdges = finder.findPath(graphModel, src, dst);

        // 设置路径中的边为红色
        for (DefaultWeightedEdge e : pathEdges) {
            Object cell = edgeToCellMap.get(e);
            if (cell != null) {
                graphUI.setCellStyle("strokeColor=red", new Object[] { cell });
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
