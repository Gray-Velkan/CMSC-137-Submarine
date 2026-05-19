package edu.cmsc137.submarine.network;

public class WelcomePacket implements LobbyPacket {
    private static final long serialVersionUID = 1L;

    private final String playerId;

    public WelcomePacket(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }
}
