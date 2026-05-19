package edu.cmsc137.submarine.network;

import edu.cmsc137.submarine.core.GameState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extended GameState that supports multiple remote players.
 * Maintains state for all players connected to the game.
 */
public class MultiplayerGameState extends GameState {
    private final Map<Integer, PlayerState> remotePlayerStates;
    private final Object stateLock = new Object();
    private int localPlayerId;

    public MultiplayerGameState(int worldWidth, int worldHeight, double initialTimeSeconds) {
        super(worldWidth, worldHeight, initialTimeSeconds);
        this.remotePlayerStates = new ConcurrentHashMap<>();
        this.localPlayerId = -1;
    }

    public void setLocalPlayerId(int playerId) {
        this.localPlayerId = playerId;
    }

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    public void updateRemotePlayerState(int playerId, PlayerState state) {
        synchronized (stateLock) {
            remotePlayerStates.put(playerId, state);
        }
    }

    public PlayerState getRemotePlayerState(int playerId) {
        synchronized (stateLock) {
            return remotePlayerStates.get(playerId);
        }
    }

    public Collection<PlayerState> getAllRemotePlayerStates() {
        synchronized (stateLock) {
            List<PlayerState> visiblePlayers = new ArrayList<>();
            for (PlayerState state : remotePlayerStates.values()) {
                if (state.playerId != localPlayerId) {
                    visiblePlayers.add(state);
                }
            }
            return visiblePlayers;
        }
    }

    public void removeRemotePlayer(int playerId) {
        synchronized (stateLock) {
            remotePlayerStates.remove(playerId);
        }
    }

    public int getRemotePlayerCount() {
        synchronized (stateLock) {
            int count = 0;
            for (PlayerState state : remotePlayerStates.values()) {
                if (state.playerId != localPlayerId) {
                    count++;
                }
            }
            return count;
        }
    }

    public void updateFromSnapshot(GameStateSnapshot snapshot) {
        if (snapshot != null) {
            synchronized (stateLock) {
                setBuoyancy(snapshot.submarineBuoyancy);
                setTimeRemainingSeconds(snapshot.timeRemainingSeconds);
                setSank(snapshot.sank);

                if (snapshot.playerStates != null) {
                    remotePlayerStates.clear();
                    remotePlayerStates.putAll(snapshot.playerStates);
                }
            }
        }
    }
}
