package edu.cmsc137.submarine.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameStateSnapshotPacket implements LobbyPacket {
    private static final long serialVersionUID = 1L;

    private final List<PlayerSnapshot> players;

    public GameStateSnapshotPacket(List<PlayerSnapshot> players) {
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
    }

    public List<PlayerSnapshot> getPlayers() {
        return players;
    }
}