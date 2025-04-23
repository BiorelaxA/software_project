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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    private SwingWorker<Void, String> walkWorker;
    private JButton walkBtn;

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
        walkBtn = new JButton("随机游走");
        panel1.add(walkBtn); // 将它加入到你已有的 panel1 或者其它合适的容器中

        // —— 监听器 —— //
        walkBtn.addActionListener(e -> {
            if (walkWorker == null || walkWorker.isDone()) {
                startRandomWalk();
            } else {
                // 正在运行，点击则取消
                walkWorker.cancel(true);
            }
        });

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

    private void saveWalkToFile(List<String> visitedNodes) {
        // 弹出保存对话框
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("保存随机游走结果为文本文件");
            if (chooser.showSaveDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try (PrintWriter out = new PrintWriter(f)) {
                    for (String node : visitedNodes) {
                        out.println(node);
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(MainFrame.this, "写入失败：" + e.getMessage());
                }
            }
        });
    }

    private void startRandomWalk() {
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }
        // 重置状态
        resultTextArea.setText("");
        walkBtn.setText("停止游走");

        walkWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Random rnd = new Random();
                Set<DefaultWeightedEdge> seenEdges = new HashSet<>();
                List<String> visited = new ArrayList<>();

                // 1. 随机选起点
                List<String> verts = new ArrayList<>(graphModel.vertexSet());
                String current = verts.get(rnd.nextInt(verts.size()));
                visited.add(current);
                publish(current);

                // 2. 随机游走
                while (!isCancelled()) {
                    Set<DefaultWeightedEdge> outs = graphModel.outgoingEdgesOf(current);
                    if (outs.isEmpty()) {
                        break; // 无出边
                    }
                    // 随机选一条出边
                    List<DefaultWeightedEdge> edgeList = new ArrayList<>(outs);
                    DefaultWeightedEdge e = edgeList.get(rnd.nextInt(edgeList.size()));
                    // 遇到已访问过的边则停止
                    if (!seenEdges.add(e)) {
                        break;
                    }
                    // 记录下一个节点
                    String next = graphModel.getEdgeTarget(e);
                    visited.add(next);
                    publish(next);
                    current = next;
                    Thread.sleep(200); // 让 UI 有缓冲，也能响应取消
                }
                // 3. 写入文件
                saveWalkToFile(visited);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                // 将每批 publish 的节点追加到文本区
                for (String node : chunks) {
                    resultTextArea.append(node + "\n");
                }
            }

            @Override
            protected void done() {
                walkBtn.setText("随机游走");
                if (isCancelled()) {
                    JOptionPane.showMessageDialog(MainFrame.this, "随机游走已停止");
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this, "随机游走完成，结果已保存");
                }
            }
        };

        walkWorker.execute();
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
        // 先检查两个单词都在图中
        if (graphModel == null
                || !graphModel.containsVertex(w1)
                || !graphModel.containsVertex(w2)) {
            return Collections.emptyList();
        }

        List<String> bridges = new ArrayList<>();
        // 遍历 w1 的所有出边
        for (DefaultWeightedEdge e : graphModel.outgoingEdgesOf(w1)) {
            String mid = graphModel.getEdgeTarget(e);
            // 如果存在 mid -> w2 的边，则 mid 是桥接词
            if (graphModel.containsEdge(mid, w2)) {
                bridges.add(mid);
            }
        }
        return bridges;
    }

    /**
     * 对输入文本中每对相邻单词尝试插入桥接词：
     * — 如果 findBridgeWords 返回非空，则在两词之间依次插入所有桥接词；
     * — 如果返回空，则仅在两词之间保留一个空格，不插入额外内容。
     *
     * @param text 用户输入的原始文本（已统一为小写并按空白切分）
     * @return 插入桥接词后的新文本
     */
    private String insertBridgeWords(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String[] ws = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ws.length; i++) {
            sb.append(ws[i]);
            // 只要不是最后一个词，就处理 ws[i] 与 ws[i+1]
            if (i < ws.length - 1) {
                String next = ws[i + 1];
                // 先检查节点是否都在图中
                List<String> bridges = findBridgeWords(ws[i], next);
                if (!bridges.isEmpty()) {
                    // 插入所有桥接词
                    for (String b : bridges) {
                        sb.append(" ").append(b);
                    }
                }
                // 再加上原始的下一个词与空格
                sb.append(" ").append(next).append(" ");
                // 跳过下一个词，因为已经手动输出
                i++;
            }
        }

        return sb.toString().trim();
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
