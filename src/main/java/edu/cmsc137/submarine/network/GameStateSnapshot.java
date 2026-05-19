package edu.cmsc137.submarine.network;

import edu.cmsc137.submarine.core.ItemEntity;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of the game state for network synchronization.
 */
public class GameStateSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public Map<Integer, PlayerState> playerStates;
    public int submarineBuoyancy;
    public double timeRemainingSeconds;
    public boolean sank;
    public boolean gameOver;
    public boolean isPowerRoutedToPump;
    public List<ItemEntity> worldItems;
    public long serverTimestamp;

    public GameStateSnapshot() {
        this.playerStates = new HashMap<>();
        this.serverTimestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "GameStateSnapshot{" +
                "players=" + playerStates.size() +
                ", buoyancy=" + submarineBuoyancy +
                ", time=" + timeRemainingSeconds +
                '}';
    }
}
