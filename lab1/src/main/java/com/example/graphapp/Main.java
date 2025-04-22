package com.example.graphapp;

import javax.swing.SwingUtilities;

import com.example.graphapp.ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}