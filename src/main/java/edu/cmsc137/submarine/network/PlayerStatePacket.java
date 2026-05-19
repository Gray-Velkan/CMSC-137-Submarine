package edu.cmsc137.submarine.network;

public class PlayerStatePacket implements LobbyPacket {
    private static final long serialVersionUID = 1L;

    private final String playerId;
    private final double x;
    private final double y;
    private final int facingX;
    private final int facingY;

    public PlayerStatePacket(String playerId, double x, double y, int facingX, int facingY) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.facingX = facingX;
        this.facingY = facingY;
    }

    public String getPlayerId() {
        return playerId;
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
}