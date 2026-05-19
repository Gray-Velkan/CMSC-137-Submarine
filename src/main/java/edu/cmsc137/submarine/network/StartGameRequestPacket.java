package edu.cmsc137.submarine.network;

public class StartGameRequestPacket implements LobbyPacket {
    private static final long serialVersionUID = 1L;

    private final String requesterId;

    public StartGameRequestPacket(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterId() {
        return requesterId;
    }
}
