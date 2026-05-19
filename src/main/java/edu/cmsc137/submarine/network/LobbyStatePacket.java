package edu.cmsc137.submarine.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LobbyStatePacket implements LobbyPacket {
    private static final long serialVersionUID = 1L;

    private final List<String> players;
    private final int requiredPlayers;
    private final String hostPlayerId;

    public LobbyStatePacket(List<String> players, int requiredPlayers, String hostPlayerId) {
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
        this.requiredPlayers = requiredPlayers;
        this.hostPlayerId = hostPlayerId;
    }

    public List<String> getPlayers() {
        return players;
    }

    public int getRequiredPlayers() {
        return requiredPlayers;
    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }
}
