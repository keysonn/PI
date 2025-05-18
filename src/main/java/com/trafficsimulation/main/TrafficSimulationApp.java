package com.trafficsimulation.main;

import javax.swing.SwingUtilities;
import com.trafficsimulation.gui.MainFrame;

public class TrafficSimulationApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            }
        });
    }
}