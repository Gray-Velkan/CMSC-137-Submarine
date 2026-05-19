package edu.cmsc137.submarine.network;

import java.io.Serializable;

public class PlayerSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String playerId;
    private final String playerName;
    private final double x;
    private final double y;
    private final int facingX;
    private final int facingY;
    private final boolean host;

    public PlayerSnapshot(String playerId, String playerName, double x, double y, int facingX, int facingY, boolean host) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.x = x;
        this.y = y;
        this.facingX = facingX;
        this.facingY = facingY;
        this.host = host;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getFacingX() {
        return facingX;
    }

    public int getFacingY() {
        return facingY;
    }

    public boolean isHost() {
        return host;
    }
}