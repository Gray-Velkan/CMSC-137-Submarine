package edu.cmsc137.submarine.network;

import edu.cmsc137.submarine.core.ItemType;
import java.io.Serializable;

/**
 * Serializable player state for network transmission.
 */
public class PlayerState implements Serializable {
    private static final long serialVersionUID = 1L;

    public int playerId;
    public String playerName;
    public double x;
    public double y;
    public int facingX;
    public int facingY;
    public ItemType currentItem;
    public boolean pumping;
    public boolean interactPressed;
    public boolean isActive;

    public PlayerState() {
        this.isActive = true;
    }

    public PlayerState(int playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.isActive = true;
    }

    @Override
    public String toString() {
        return "PlayerState{" +
                "playerId=" + playerId +
                ", name='" + playerName + '\'' +
                ", pos=(" + x + "," + y + ")" +
                '}';
    }
}
