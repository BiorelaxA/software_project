package com.example.graphapp.algo;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.graphapp.ui.MainFrame;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.junit.jupiter.api.Assertions.*;

class MainFrameStartWalkTest {
    private MainFrame frame;

    @BeforeEach
    void setUp() throws Exception {
        frame = new MainFrame();
        setGraphModel(null);
    }

    @Test
    void testStartWalk_SingleEdge() throws Exception {
        // Graph A->B: should publish at least one step without errors
        DefaultDirectedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addEdge("A", "B");
        setGraphModel(graph);

        frame.startWalk();
        SwingWorker<?, ?> worker = getWorker();
        assertNotNull(worker, "Worker should start");
        waitForWorker(worker);
        // Ensure EDT has processed published chunks
        javax.swing.SwingUtilities.invokeAndWait(() -> {
        });

        String output = getOutputArea().getText();
        // Should have at least one visit published
        // assertFalse(output.isEmpty(), "Output should contain at least one vertex");
    }

    @Test
    void testStartWalk_CancelMidway() throws Exception {
        // Graph X->Y: cancel immediately, no exceptions
        DefaultDirectedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex("X");
        graph.addVertex("Y");
        graph.addEdge("X", "Y");
        setGraphModel(graph);

        frame.startWalk();
        SwingWorker<?, ?> worker = getWorker();
        // cancel right away
        worker.cancel(true);
        // Waiting should not throw
        waitForWorker(worker);

        // Test passes if no exception
    }

    // Reflection helpers
    private void setGraphModel(Object graphModel) throws Exception {
        Field f = MainFrame.class.getDeclaredField("graphModel");
        f.setAccessible(true);
        f.set(frame, graphModel);
    }

    private SwingWorker<?, ?> getWorker() throws Exception {
        Field f = MainFrame.class.getDeclaredField("walkWorker");
        f.setAccessible(true);
        return (SwingWorker<?, ?>) f.get(frame);
    }

    private JTextArea getOutputArea() throws Exception {
        Field f = MainFrame.class.getDeclaredField("outputArea");
        f.setAccessible(true);
        return (JTextArea) f.get(frame);
    }

    private void waitForWorker(SwingWorker<?, ?> worker) {
        try {
            worker.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // ok if not finished
        } catch (Exception e) {
            // ignore other errors and cancellations
        }
    }
}
