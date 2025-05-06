package com.example.graphapp.ui;

import com.example.graphapp.input.GraphBuilder;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

import com.example.graphapp.algo.DijkstraShortestPathService;
import com.example.graphapp.algo.PageRankService;
import com.example.graphapp.algo.ShortestPathFinder;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
// import org.jgrapht.nio.dot.DOTExp;// import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.Attribute;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;

/**
 * 主界面：加载图、最短路径、随机游走及桥接词功能
 */
public class MainFrame extends JFrame {
    private Graph<String, DefaultWeightedEdge> graphModel;
    private ShortestPathFinder pathFinder = new DijkstraShortestPathService();

    // UI 组件
    private JTextField srcField;
    private JTextField dstField;
    private JButton pathBtn;
    private JButton walkBtn;
    private JButton prBtn;
    private JTextField bridgeSrcField;
    private JTextField bridgeDstField;
    private JButton bridgeBtn;
    private JTextArea outputArea; // 用于显示最短路径、随机游走、桥接词结果
    private JTextArea insertInputArea; // 桥接词输入
    private JTextArea insertResultArea; // 桥接词插入结果
    private SwingWorker<Void, String> walkWorker;

    public MainFrame() {
        super("Graph Visualizer");
        setLayout(new BorderLayout(5, 5));

        // 顶部：加载图文件按钮
        JButton loadBtn = new JButton("加载图文件");
        loadBtn.addActionListener(e -> chooseLoadAndExport());
        add(loadBtn, BorderLayout.NORTH);

        // 中部：左侧图形，右侧功能与输出
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // 左：图形占位（可扩展为 mxGraphComponent）
        JPanel graphPanel = new JPanel(new BorderLayout());
        graphPanel.setBorder(BorderFactory.createTitledBorder("图视图（暂不渲染）"));
        mainSplit.setLeftComponent(graphPanel);

        // 右：功能面板与输出
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(new JLabel("源:"), gbc);
        srcField = new JTextField(8);
        gbc.gridx = 1;
        controlPanel.add(srcField, gbc);
        gbc.gridx = 2;
        controlPanel.add(new JLabel("终 (留空=所有):"), gbc);
        dstField = new JTextField(8);
        gbc.gridx = 3;
        controlPanel.add(dstField, gbc);
        pathBtn = new JButton("最短路径");
        gbc.gridx = 4;
        controlPanel.add(pathBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        controlPanel.add(new JLabel("桥接源:"), gbc);
        bridgeSrcField = new JTextField(8);
        gbc.gridx = 1;
        controlPanel.add(bridgeSrcField, gbc);
        gbc.gridx = 2;
        controlPanel.add(new JLabel("桥接目标:"), gbc);
        bridgeDstField = new JTextField(8);
        gbc.gridx = 3;
        controlPanel.add(bridgeDstField, gbc);
        bridgeBtn = new JButton("寻找桥接词");
        gbc.gridx = 4;
        controlPanel.add(bridgeBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 5;
        walkBtn = new JButton("随机游走");
        controlPanel.add(walkBtn, gbc);
        prBtn = new JButton("计算 PageRank");
        controlPanel.add(prBtn, gbc);
        outputArea = new JTextArea(20, 30);
        outputArea.setEditable(false);

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.add(controlPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        mainSplit.setRightComponent(rightPanel);
        mainSplit.setResizeWeight(0.5);
        add(mainSplit, BorderLayout.CENTER);

        // 底部：桥接词插入区
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("桥接词插入"));
        insertInputArea = new JTextArea(3, 40);
        insertResultArea = new JTextArea(3, 40);
        insertResultArea.setEditable(false);
        JButton insertBtn = new JButton("插入桥接词");
        insertBtn.addActionListener(e -> doInsertBridge());
        bottomPanel.add(new JScrollPane(insertInputArea), BorderLayout.NORTH);
        bottomPanel.add(insertBtn, BorderLayout.CENTER);
        bottomPanel.add(new JScrollPane(insertResultArea), BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // 事件绑定
        pathBtn.addActionListener(e -> computePaths());
        bridgeBtn.addActionListener(e -> showBridge());
        walkBtn.addActionListener(e -> toggleWalk());
        prBtn.addActionListener(e -> computePageRank());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void computePageRank() {
        outputArea.setText("");
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }
        // 可选：根据 insertInputArea 中词汇设定初始PR权重
        Set<String> boost = new HashSet<>(Arrays.asList(insertInputArea.getText().split("\\s+")));
        Map<String, Double> pr = new PageRankService().computePageRank(graphModel, 0.85, 100, 1e-6);
        pr.replaceAll((v, val) -> boost.contains(v) ? val * 1.2 : val);
        pr.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> outputArea.append(e.getKey() + ": " + String.format("%.4f", e.getValue()) + "\n"));
    }

    private void chooseLoadAndExport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择输入文件");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File inFile = fileChooser.getSelectedFile();
            try {
                graphModel = new GraphBuilder().buildGraph(inFile);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "读取文件失败：" + ex.getMessage());
                return;
            }

            JFileChooser exportChooser = new JFileChooser();
            exportChooser.setDialogTitle("保存为图像文件");
            if (exportChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File outFile = exportChooser.getSelectedFile();
                try {
                    exportGraphAsImage(outFile);
                    JOptionPane.showMessageDialog(this, "导出成功：" + outFile.getAbsolutePath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage());
                }
            }
        }
    }

    /**
     * 将 graphModel 导出为 DOT 格式字符串
     */
    /**
     * 将 graphModel 导出为 DOT 格式字符串，
     * 仅保留字母数字字符，其它全部忽略
     */
    private String exportGraphToDotString() {
        // 顶点 ID 及标签处理：只保留 a–z, A–Z, 0–9
        Function<String, String> sanitize = v -> {
            // 删除所有非字母数字字符
            String cleaned = v.replaceAll("[^A-Za-z0-9]", "");
            // 如果清空了，就用一个占位符，避免空 ID
            return cleaned.isEmpty() ? "_empty_" : cleaned;
        };
        // 边标签（权重）——直接保留数字字符
        Function<DefaultWeightedEdge, String> edgeLabelProv = e -> String.valueOf((int) graphModel.getEdgeWeight(e));

        // 显式指定泛型，避免类型推断失败
        DOTExporter<String, DefaultWeightedEdge> exporter = new DOTExporter<String, DefaultWeightedEdge>(sanitize);

        // 顶点属性：label 只用清洗后的字符串
        exporter.setVertexAttributeProvider(v -> {
            Map<String, Attribute> m = new LinkedHashMap<>();
            String label = sanitize.apply(v);
            m.put("label", DefaultAttribute.createAttribute(label));
            return m;
        });

        // 边属性：label 为权重（数字字符串，本身不含非法字符）
        exporter.setEdgeAttributeProvider(e -> {
            Map<String, Attribute> m = new LinkedHashMap<>();
            m.put("label", DefaultAttribute.createAttribute(edgeLabelProv.apply(e)));
            return m;
        });

        StringWriter writer = new StringWriter();
        exporter.exportGraph(graphModel, writer);
        return writer.toString();
    }

    /**
     * 使用 graphviz-java 将 DOT 字符串渲染为图像
     */
    private void exportGraphAsImage(File outFile) throws IOException {
        String dot = exportGraphToDotString();
        // 提取文件扩展名
        String name = outFile.getName();
        String ext = "";
        int idx = name.lastIndexOf('.');
        if (idx > 0 && idx < name.length() - 1) {
            ext = name.substring(idx + 1).toLowerCase();
        }
        // 根据扩展名选择对应的 Format
        Format format;
        switch (ext) {
            case "png":
                format = Format.PNG;
                break;
            case "svg":
                format = Format.SVG;
                break;
            case "svg_standalone":
                format = Format.SVG_STANDALONE;
                break;
            case "dot":
                format = Format.DOT;
                break;
            case "plain":
                format = Format.PLAIN;
                break;
            case "plain-ext":
                format = Format.PLAIN_EXT;
                break;
            case "json":
                format = Format.JSON;
                break;
            case "json0":
                format = Format.JSON0;
                break;
            case "imap":
                format = Format.IMAP;
                break;
            case "cmapx":
                format = Format.CMAPX;
                break;
            case "ps":
                format = Format.PS;
                break;
            case "ps2":
                format = Format.PS2;
                break;
            default:
                throw new IllegalArgumentException(
                        "不支持的输出格式: " + ext + ". 请使用 png/svg/svg_standalone/dot/plain/json 等格式");
        }
        // 渲染并写入文件
        // Graphviz.useEngine(new GraphvizCmdLineEngine());
        // new GraphvizCmdLineEngine().timeout(10, TimeUnit.MINUTES)
        Graphviz.useEngine(
                new GraphvizCmdLineEngine().timeout(10, TimeUnit.MINUTES));
        Graphviz.fromString(dot)
                .render(format)
                .toFile(outFile);
    }

    private void computePaths() {
        outputArea.setText("");
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }
        String src = srcField.getText().trim();
        String dst = dstField.getText().trim();
        if (!graphModel.containsVertex(src)) {
            JOptionPane.showMessageDialog(this, "源节点不存在:" + src);
            return;
        }
        if (dst.isEmpty() || !graphModel.containsVertex(dst)) {
            for (String v : graphModel.vertexSet())
                displayPath(src, v);
        } else {
            displayPath(src, dst);
        }
    }

    private void displayPath(String src, String dst) {
        List<DefaultWeightedEdge> edges = pathFinder.findPath(graphModel, src, dst);
        String line;
        if (edges == null || edges.isEmpty()) {
            line = src + " -> " + dst + ": 无路径";
        } else {
            List<String> verts = edges.stream()
                    .map(e -> graphModel.getEdgeSource(e))
                    .collect(Collectors.toList());
            verts.add(dst);
            line = String.join(" -> ", verts);
        }
        outputArea.append(line + "\n");
    }

    private void showBridge() {
        outputArea.setText("");
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }
        String w1 = bridgeSrcField.getText().trim();
        String w2 = bridgeDstField.getText().trim();
        if (!graphModel.containsVertex(w1) || !graphModel.containsVertex(w2)) {
            JOptionPane.showMessageDialog(this, "图中不存在" + w1 + "或" + w2);
            return;
        }
        List<String> bridges = findBridgeWords(w1, w2);
        if (bridges.isEmpty())
            outputArea.append("无桥接词\n");
        else
            outputArea.append("桥接词: " + String.join(", ", bridges) + "\n");
    }

    private List<String> findBridgeWords(String w1, String w2) {
        List<String> res = new ArrayList<>();
        for (DefaultWeightedEdge e : graphModel.outgoingEdgesOf(w1)) {
            String mid = graphModel.getEdgeTarget(e);
            if (graphModel.containsEdge(mid, w2))
                res.add(mid);
        }
        return res;
    }

    private void toggleWalk() {
        if (walkWorker == null || walkWorker.isDone())
            startWalk();
        else
            walkWorker.cancel(true);
    }

    private void startWalk() {
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }
        outputArea.setText("");
        walkBtn.setText("停止游走");
        walkWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Random rnd = new Random();
                Set<DefaultWeightedEdge> seen = new HashSet<>();
                List<String> visited = new ArrayList<>();
                List<String> verts = new ArrayList<>(graphModel.vertexSet());
                String cur = verts.get(rnd.nextInt(verts.size()));
                publish(cur);
                visited.add(cur);
                while (!isCancelled()) {
                    var outs = graphModel.outgoingEdgesOf(cur);
                    if (outs.isEmpty())
                        break;
                    var edges = new ArrayList<>(outs);
                    var e = edges.get(rnd.nextInt(edges.size()));
                    if (!seen.add(e))
                        break;
                    cur = graphModel.getEdgeTarget(e);
                    publish(cur);
                    visited.add(cur);
                    Thread.sleep(200);
                }
                saveWalk(visited);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String n : chunks)
                    outputArea.append(n + "\n");
            }

            @Override
            protected void done() {
                walkBtn.setText("随机游走");
                JOptionPane.showMessageDialog(MainFrame.this,
                        isCancelled() ? "游走已停止" : "游走完成，已保存");
            }
        };
        walkWorker.execute();
    }

    private void saveWalk(List<String> visited) {
        SwingUtilities.invokeLater(() -> {
            JFileChooser c = new JFileChooser();
            c.setDialogTitle("保存游走结果");
            if (c.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = c.getSelectedFile();
                try (PrintWriter out = new PrintWriter(f)) {
                    visited.forEach(out::println);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "保存失败:" + e.getMessage());
                }
            }
        });
    }

    private void doInsertBridge() {
        if (graphModel == null) {
            JOptionPane.showMessageDialog(this, "请先加载图");
            return;
        }
        String input = insertInputArea.getText().trim().toLowerCase();
        // 插入桥接词逻辑（与之前相同）
        String[] ws = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ws.length - 1; i++) {
            sb.append(ws[i]);
            List<String> br = findBridgeWords(ws[i], ws[i + 1]);
            for (String b : br)
                sb.append(" ").append(b);
            sb.append(" ").append(ws[i + 1]).append(" ");
        }
        insertResultArea.setText(sb.toString().trim());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
