package edu.cmsc137.submarine.network;

public class ClientHelloPacket implements LobbyPacket {
    private static final long serialVersionUID = 1L;

    private final String playerName;

    public ClientHelloPacket(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
