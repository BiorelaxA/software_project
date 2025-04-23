package com.example.graphapp.ui;

import com.example.graphapp.input.GraphBuilder;
import com.example.graphapp.algo.ShortestPathFinder;
import com.example.graphapp.algo.DijkstraShortestPathService;
import com.example.graphapp.algo.PageRankService;
import com.example.graphapp.bridge.findbridge;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private JTextArea newTextArea;
    private JTextArea resultTextArea;

    public MainFrame() {
        super("GraphVisualizer");
        // 上部按钮
        JButton loadBtn = new JButton("加载文件");
        JButton pathBtn = new JButton("最短路径");
        JButton findBridgeBtn = new JButton("寻找连接字");
        JButton prk = new JButton("计算pagerank");
        JTextField srcField = new JTextField(10);
        JTextField dstField = new JTextField(10);
        JTextField word1Field = new JTextField(10);
        JTextField word2Field = new JTextField(10);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        JPanel panel1 = new JPanel();
        panel1.add(loadBtn);
        panel1.add(new JLabel("源:"));
        panel1.add(srcField);
        panel1.add(new JLabel("终:"));
        panel1.add(dstField);
        panel1.add(pathBtn);
        panel1.add(new JLabel("桥接词:"));
        panel1.add(word1Field);
        panel1.add(new JLabel("->"));
        panel1.add(word2Field);
        panel1.add(findBridgeBtn);
        panel1.add(prk);
        topPanel.add(panel1);

        // 中部图形
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(graphComponent, BorderLayout.CENTER);

        // 下部插入桥接词文本
        newTextArea = new JTextArea(4, 50);
        resultTextArea = new JTextArea(4, 50);
        resultTextArea.setEditable(false);
        JButton insertBtn = new JButton("插入桥接词");
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("桥接词插入"));
        bottomPanel.add(new JScrollPane(newTextArea), BorderLayout.NORTH);
        bottomPanel.add(insertBtn, BorderLayout.CENTER);
        bottomPanel.add(new JScrollPane(resultTextArea), BorderLayout.SOUTH);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // 按钮监听逻辑
        loadBtn.addActionListener(e -> chooseAndLoad());
        pathBtn.addActionListener(e -> showShortestPath(srcField.getText().trim().toLowerCase(),
                dstField.getText().trim().toLowerCase()));
        findBridgeBtn.addActionListener(e -> showBridge(word1Field.getText().trim().toLowerCase(),
                word2Field.getText().trim().toLowerCase()));
        prk.addActionListener(e -> showPageRank());
        insertBtn.addActionListener(e -> {
            String input = newTextArea.getText().trim().toLowerCase();
            String out = insertBridgeWords(input);
            resultTextArea.setText(out);
        });
    }

    private void chooseAndLoad() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                graphModel = new GraphBuilder().buildGraph(file);
                renderGraph(graphModel);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "读取文件失败：" + ex.getMessage());
            }
        }
    }

    private void renderGraph(Graph<String, DefaultWeightedEdge> g) {
        Object parent = graphUI.getDefaultParent();
        // 清除旧有单元(cell)
        Object[] cells = graphUI.getChildCells(parent, true, true);
        if (cells != null && cells.length > 0) {
            graphUI.removeCells(cells);
        }
        edgeToCellMap.clear();
        Map<String, Object> vMap = new HashMap<>();
        int x = 20, y = 20;
        graphUI.getModel().beginUpdate();
        try {
            // 插入顶点
            for (String v : g.vertexSet()) {
                Object cell = graphUI.insertVertex(parent, null, v, x, y, 80, 30);
                vMap.put(v, cell);
                x += 100;
                if (x > 800) {
                    x = 20;
                    y += 70;
                }
            }
            // 插入边
            for (DefaultWeightedEdge e : g.edgeSet()) {
                String s = g.getEdgeSource(e);
                String t = g.getEdgeTarget(e);
                Object cell = graphUI.insertEdge(parent, null, (int) g.getEdgeWeight(e), vMap.get(s), vMap.get(t));
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
        for (Object cell : edgeToCellMap.values())
            graphUI.setCellStyle("strokeColor=black", new Object[] { cell });
        List<DefaultWeightedEdge> edges = new DijkstraShortestPathService().findPath(graphModel, src, dst);
        for (DefaultWeightedEdge e : edges)
            graphUI.setCellStyle("strokeColor=red", new Object[] { edgeToCellMap.get(e) });
    }

    private void showBridge(String w1, String w2) {
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }
        if (!graphModel.containsVertex(w1) || !graphModel.containsVertex(w2)) {
            JOptionPane.showMessageDialog(this, "图中不存在" + w1 + "或" + w2);
            return;
        }
        List<String> bridges = findBridgeWords(w1, w2);
        if (bridges.isEmpty())
            JOptionPane.showMessageDialog(this, "无桥接词");
        else
            JOptionPane.showMessageDialog(this, "桥接词：" + String.join(", ", bridges));
    }

    private List<String> findBridgeWords(String w1, String w2) {
        List<String> list = new ArrayList<>();
        for (DefaultWeightedEdge e : graphModel.outgoingEdgesOf(w1)) {
            String mid = graphModel.getEdgeTarget(e);
            if (graphModel.containsEdge(mid, w2))
                list.add(mid);
        }
        return list;
    }

    private String insertBridgeWords(String text) {
        if (text.isEmpty())
            return "";
        String[] ws = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ws.length; i++) {
            sb.append(ws[i]);
            if (i < ws.length - 1) {
                List<String> b = findBridgeWords(ws[i], ws[i + 1]);
                for (String t : b)
                    sb.append(" ").append(t);
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private void showPageRank() {
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }
        Map<String, Double> pr = new PageRankService().computePageRank(graphModel, 0.85, 100, 1e-6);
        List<Map.Entry<String, Double>> sorted = pr.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        sorted.forEach(e -> sb.append(e.getKey()).append(": ")
                .append(String.format("%.4f", e.getValue())).append("\n"));
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "PageRank 结果", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}

// public static void main(String[] args) {
// SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
// }
// }
