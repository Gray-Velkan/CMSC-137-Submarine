package edu.cmsc137.submarine;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class GameLauncher {
    private GameLauncher() {
        // utility class
    }

    public static void main(String[] args) {
        // create ui on the swing event dispatch thread
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CMSC 137 Submarine - Milestone 1");
            GamePanel panel = new GamePanel();

            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // stop loop so the thread exits cleanly
                    panel.stopGameLoop();
                }
            });

            frame.setVisible(true);
        });
    }
}
