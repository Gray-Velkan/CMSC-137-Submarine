package edu.cmsc137.submarine.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameStartPacket implements LobbyPacket {
    private static final long serialVersionUID = 1L;

    private final int requiredPlayers;
    private final List<PlayerSnapshot> players;

    public GameStartPacket(int requiredPlayers, List<PlayerSnapshot> players) {
        this.requiredPlayers = requiredPlayers;
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
    }

    public int getRequiredPlayers() {
        return requiredPlayers;
    }

    public List<PlayerSnapshot> getPlayers() {
        return players;
    }
}
