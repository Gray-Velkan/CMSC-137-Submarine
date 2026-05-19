package edu.cmsc137.submarine;

import edu.cmsc137.submarine.ui.LobbyMenu;
import javax.swing.SwingUtilities;

public final class GameLauncher {
    private GameLauncher() {
        // utility class
    }

    public static void main(String[] args) {
        // create ui on the swing event dispatch thread
        SwingUtilities.invokeLater(() -> {
            // show lobby instead of directly launching the game panel
            LobbyMenu.showLobby();
        });
    }
}
