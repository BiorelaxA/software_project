package com.example.graphapp.ui;

import com.example.graphapp.input.GraphBuilder;
import com.example.graphapp.algo.ShortestPathFinder;
import com.example.graphapp.algo.DijkstraShortestPathService;
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

        // 原有组件
        JButton loadBtn = new JButton("加载文件");
        JButton pathBtn = new JButton("最短路径");
        JTextField srcField = new JTextField(10);
        JTextField dstField = new JTextField(10);

        // 新增组件
        JButton findBridgeBtn = new JButton("寻找连接字");
        JTextField word1Field = new JTextField(10);
        JTextField word2Field = new JTextField(10);
        JTextArea newTextArea = new JTextArea(5, 60); // 新增：原始文本输入
        JTextArea resultTextArea = new JTextArea(5, 60); // 新增：结果展示

        // 顶部面板，使用 BoxLayout 垂直排列两个子面板
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // 第一个子面板：加载文件和最短路径
        JPanel panel1 = new JPanel();
        panel1.add(loadBtn);
        panel1.add(new JLabel("源:"));
        panel1.add(srcField);
        panel1.add(new JLabel("终:"));
        panel1.add(dstField);
        panel1.add(pathBtn);

        // 第二个子面板：寻找连接字
        JPanel panel2 = new JPanel();
        panel2.add(new JLabel("词1:"));
        panel2.add(word1Field);
        panel2.add(new JLabel("词2:"));
        panel2.add(word2Field);
        panel2.add(findBridgeBtn);

        // 将两个子面板添加到顶部面板
        topPanel.add(panel1);
        topPanel.add(panel2);

        // 设置布局
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(graphComponent, BorderLayout.CENTER);
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        resultTextArea.setEditable(false);
        JButton insertBtn = new JButton("插入桥接词");

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("桥接词插入"));
        bottomPanel.add(new JScrollPane(newTextArea), BorderLayout.NORTH);
        bottomPanel.add(insertBtn, BorderLayout.CENTER);
        bottomPanel.add(new JScrollPane(resultTextArea), BorderLayout.SOUTH);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        // —— 按钮监听 —— //
        insertBtn.addActionListener(evt -> {
            String input = newTextArea.getText().trim().toLowerCase();
            if (graphModel == null) {
                JOptionPane.showMessageDialog(this, "请先加载图！");
                return;
            }
            String out = insertBridgeWords(input);
            resultTextArea.setText(out);
        });

        // 按钮事件监听器
        loadBtn.addActionListener(e -> chooseAndLoad());
        pathBtn.addActionListener(e -> showShortestPath(
                srcField.getText().trim().toLowerCase(),
                dstField.getText().trim().toLowerCase()));
        findBridgeBtn.addActionListener(e -> findbridge(
                word1Field.getText().trim().toLowerCase(),
                word2Field.getText().trim().toLowerCase()));
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

    private void findbridge(String word1, String word2) {
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }
        findbridge f = new findbridge();
        List<String> res = f.findBridgeWords(graphModel, word1, word2);
        if (!graphModel.containsVertex(word1) || !graphModel.containsVertex(word2)) {
            JOptionPane.showMessageDialog(this, "图中不存在 " + word1 + " 或 " + word2 + "！");
        } else if (res.isEmpty()) {
            JOptionPane.showMessageDialog(this, "从 " + word1 + " 到 " + word2 + " 没有桥接词！");
        } else {
            JOptionPane.showMessageDialog(this, "从 " + word1 + " 到 " + word2 + " 的桥接词有: " + String.join(", ", res));
            highlightBridgeWords(res);
        }
    }

    private void highlightBridgeWords(List<String> bridgeWords) {
        Object parent = graphUI.getDefaultParent();
        graphUI.getModel().beginUpdate();
        try {
            for (String word : bridgeWords) {
                for (Object cell : graphUI.getChildVertices(parent)) {
                    if (word.equals(graphUI.getModel().getValue(cell))) {
                        graphUI.setCellStyle("fillColor=red", new Object[] { cell });
                    }
                }
            }
        } finally {
            graphUI.getModel().endUpdate();
        }
    }

    /**
     * 对输入文本中每对相邻单词尝试插入桥接词。
     * 如 “w1 w2” 且存在桥接词 [b1, b2]，
     * 则输出 “w1 b1 b2 w2”，否则保持 “w1 w2”。
     */
    private String insertBridgeWords(String text) {
        if (text.isEmpty())
            return "";

        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            sb.append(words[i]);
            // 若不是最后一个词，处理与下词之间的桥接
            if (i < words.length - 1) {
                String w1 = words[i], w2 = words[i + 1];
                List<String> bridges = findBridgeWords(w1, w2);
                if (!bridges.isEmpty()) {
                    // 插入所有桥接词
                    for (String b : bridges) {
                        sb.append(" ").append(b);
                    }
                }
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /** 复用前面定义的桥接词查找，直接对 graphModel 操作 */
    private List<String> findBridgeWords(String w1, String w2) {
        List<String> res = new ArrayList<>();
        if (!graphModel.containsVertex(w1) || !graphModel.containsVertex(w2)) {
            return res;
        }
        for (DefaultWeightedEdge e : graphModel.outgoingEdgesOf(w1)) {
            String mid = graphModel.getEdgeTarget(e);
            if (graphModel.containsEdge(mid, w2)) {
                res.add(mid);
            }
        }
        return res;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
