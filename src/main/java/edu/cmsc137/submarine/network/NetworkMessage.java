package edu.cmsc137.submarine.network;

import java.io.*;

/**
 * Represents a network message for game state synchronization.
 * Messages are serialized and sent over TCP sockets.
 */
public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        // Connection management
        JOIN_GAME,           // Client -> Server: Request to join
        JOIN_RESPONSE,       // Server -> All: New player joined
        PLAYER_DISCONNECTED, // Server -> All: Player disconnected
        
        // Game state
        PLAYER_MOVE,         // Client -> Server: Player movement
        PLAYER_ACTION,       // Client -> Server: Player action (pump, use item)
        GAME_STATE_UPDATE,   // Server -> All: Full game state sync
        ITEM_PICKUP,         // Client -> Server: Item pickup
        ITEM_DROP,          // Client -> Server: Item drop
        
        // Game lifecycle
        START_GAME,          // Server -> All: Game starts
        GAME_OVER,          // Server -> All: Game finished
        PAUSE_GAME,         // Server <-> All: Game paused
        
        // Connection
        PING,               // Heartbeat
        DISCONNECT          // Graceful disconnect
    }

    private MessageType type;
    private int playerId;
    private long timestamp;
    private Object data;

    public NetworkMessage(MessageType type, int playerId) {
        this(type, playerId, null);
    }

    public NetworkMessage(MessageType type, int playerId, Object data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() {
        return type;
    }

    public int getPlayerId() {
        return playerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "NetworkMessage{" +
                "type=" + type +
                ", playerId=" + playerId +
                ", timestamp=" + timestamp +
                '}';
    }
}
