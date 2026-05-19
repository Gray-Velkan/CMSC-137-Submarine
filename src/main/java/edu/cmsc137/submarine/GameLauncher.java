package edu.cmsc137.submarine;

import edu.cmsc137.submarine.ui.MainFrame;
import javax.swing.SwingUtilities;

public final class GameLauncher {
    private GameLauncher() {
        // utility class
    }

    public static void main(String[] args) {
        // create ui on the swing event dispatch thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
