
module com.example.graphapp {
    requires java.desktop; // 用于 Swing GUI
    requires org.jgrapht.core; // 图构建和算法（如 Dijkstra）
    requires com.github.vlsi.mxgraph.jgraphx; // 图可视化组件 JGraphX
    requires guru.nidi.graphviz;
    requires org.jgrapht.io;

}
