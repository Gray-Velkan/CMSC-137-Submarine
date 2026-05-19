package edu.cmsc137.submarine.network;

/**
 * Network configuration constants.
 */
public class NetworkConstants {
    public static final int SERVER_PORT = 5137;
    public static final String SERVER_HOST = "localhost";
    public static final int MAX_PLAYERS = 8;
    public static final int CONNECTION_TIMEOUT_MS = 30000;
    public static final int HEARTBEAT_INTERVAL_MS = 5000;
    public static final int STATE_SYNC_INTERVAL_MS = 50; // ~20 times per second
    
    // Default localhost for development
    public static final String[] ALLOWED_HOSTS = {
        "127.0.0.1",
        "localhost",
        "0.0.0.0"
    };
    
    private NetworkConstants() {
        // utility class
    }
}
