# Submarine Survival - Networked Multiplayer Guide

## Overview

Your submarine survival game now supports networked multiplayer with up to 8 players (default MAX_PLAYERS). Players can connect from different PCs over a local network to play cooperatively.

## Architecture

### Network Components

1. **GameServer** (`network/GameServer.java`)
   - Runs on one PC (the host)
   - Manages game state and player connections
   - Broadcasts game updates to all connected clients
   - Handles player disconnections

2. **GameClient** (`network/GameClient.java`)
   - Runs on each player's PC
   - Connects to the GameServer
   - Sends local player input and receives game state updates
   - Maintains thread-safe message queues for network communication

3. **NetworkMessage** (`network/NetworkMessage.java`)
   - Serializable messages for client-server communication
   - Types: JOIN_GAME, PLAYER_MOVE, GAME_STATE_UPDATE, DISCONNECT, etc.

4. **PlayerState** (`network/PlayerState.java`)
   - Represents a player's networked state (position, facing direction, item, etc.)
   - Sent between server and clients for synchronization

5. **GameStateSnapshot** (`network/GameStateSnapshot.java`)
   - Contains full game state for synchronization
   - Includes all players' states and submarine status

### UI Components

1. **LobbyPanel** (`ui/LobbyPanel.java`)
   - Initial screen for selecting Host or Join mode
   - Enter player name and server host
   - Provides connection status feedback

2. **MainFrame** (`ui/MainFrame.java`)
   - Main application frame with card layout
   - Switches between LobbyPanel and GamePanel
   - Manages server/client lifecycle

3. **MultiplayerGamePanel** (`ui/MultiplayerGamePanel.java`)
   - Extended GamePanel for multiplayer rendering
   - Displays remote players with different colors
   - Shows player count and network status

## Network Configuration

Edit `network/NetworkConstants.java` to customize:

```java
public static final int SERVER_PORT = 5137;           // Server port
public static final String SERVER_HOST = "localhost"; // Default host
public static final int MAX_PLAYERS = 8;              // Maximum concurrent players
public static final int CONNECTION_TIMEOUT_MS = 30000;// Connection timeout
public static final int STATE_SYNC_INTERVAL_MS = 50;  // ~20 updates/second
```

## How to Play

### Starting a Game

#### Host Mode (Server)
1. Launch the game
2. Enter a player name
3. Click "Host Game"
4. Wait for other players to join
5. Game starts automatically when ready

#### Client Mode (Joining)
1. Launch the game
2. Enter a player name
3. Enter the server host IP address or "localhost" for local network
4. Click "Join Game"
5. Wait for game to start

### Game Controls
- **WASD**: Move your submarine
- **E**: Interact with tasks or pickup items
- **Q**: Drop held item
- **T**: Throw item
- **ESC**: (Optional) Return to lobby

## Network Protocol

### Message Flow

1. **Connection Phase**
   ```
   Client -> Server: JOIN_GAME message with player name
   Server -> Client: JOIN_RESPONSE with assigned player ID
   Server -> All: NEW_PLAYER announcement
   ```

2. **Game State Synchronization**
   ```
   Client -> Server: PLAYER_MOVE (dx, dy)
   Client -> Server: PLAYER_ACTION (action type)
   Server -> All: GAME_STATE_UPDATE (every 50ms)
   ```

3. **Disconnection**
   ```
   Client -> Server: DISCONNECT
   Server -> All: PLAYER_DISCONNECTED announcement
   ```

### Message Types

| Type | Direction | Purpose |
|------|-----------|---------|
| JOIN_GAME | Client → Server | Request to join game |
| JOIN_RESPONSE | Server → Client | Confirm join with player ID |
| PLAYER_MOVE | Client → Server | Send player movement |
| PLAYER_ACTION | Client → Server | Send player action |
| GAME_STATE_UPDATE | Server → All | Broadcast game state |
| PLAYER_DISCONNECTED | Server → All | Notify of player leaving |
| PING | Bidirectional | Heartbeat/keepalive |

## Performance Considerations

### Bandwidth
- Default: ~20 game state updates per second
- Each update: ~500 bytes (typical)
- Total: ~10 KB/s per player
- Adjust STATE_SYNC_INTERVAL_MS to balance responsiveness vs bandwidth

### Latency
- Network updates run on separate threads
- Game loop independent of network I/O
- Estimated latency: 50-100ms on local network

### Scalability
- Max players: 8 (configurable)
- Per-player overhead: ~2 threads (receive, send)
- Server thread pool: 1 main game loop + 1 accept thread + N client handlers

## Implementation Details

### Thread Safety
- Uses ConcurrentHashMap for connected clients
- BlockingQueue for message passing
- Synchronized locks for critical sections

### Serialization
- Java ObjectInputStream/ObjectOutputStream
- All network objects implement Serializable
- Message versioning via serialVersionUID

### Error Handling
- Socket exceptions trigger graceful disconnection
- Client reconnection attempt handling
- Server broadcasts player disconnect notices

## Testing

### Local Network Test (Single PC)
1. Terminal 1: Run game, select "Host Game"
2. Terminal 2: Run game, select "Join Game", enter "localhost"
3. Both players should connect and see each other move

### Multi-PC Test
1. PC1: Host Game
2. PC2: Join Game, enter PC1's IP address
3. Verify both players can see each other's movements

### Network Issues Test
- Disconnect network cable during gameplay
- Verify graceful reconnection handling
- Check server broadcasts disconnect notice to others

## Troubleshooting

### "Connection refused"
- Ensure server is running on the host PC
- Verify firewall allows port 5137
- Check SERVER_HOST/SERVER_PORT configuration

### "No players visible"
- Check network connectivity between PCs
- Verify both clients received JOIN_RESPONSE message
- Check server console for error messages

### Laggy gameplay
- Reduce STATE_SYNC_INTERVAL_MS for more frequent updates
- Increase if network bandwidth is limited
- Check network latency with `ping` command

### Players stuck after disconnection
- Server should auto-remove disconnected clients
- Check if PLAYER_DISCONNECTED message was received
- May need to return to lobby and rejoin

## Future Enhancements

1. **Server Persistence**: Save game state to disk
2. **Player Roles**: Different abilities based on role
3. **Chat System**: In-game communication
4. **Replay System**: Record and playback network games
5. **Advanced Compression**: Reduce bandwidth for remote players
6. **Better Latency Compensation**: Interpolation of remote player positions
7. **Game Lobbies**: Create/join named game sessions
8. **Ranked Matchmaking**: Skill-based player pairing

## Code Structure

```
src/main/java/edu/cmsc137/submarine/
├── network/
│   ├── GameServer.java              # Server implementation
│   ├── GameClient.java              # Client implementation
│   ├── NetworkMessage.java          # Message protocol
│   ├── PlayerState.java             # Player state serialization
│   ├── GameStateSnapshot.java       # Full state snapshot
│   ├── MultiplayerGameState.java    # Extended game state
│   └── NetworkConstants.java        # Configuration
└── ui/
    ├── MainFrame.java               # Application frame
    ├── LobbyPanel.java              # Lobby UI
    └── MultiplayerGamePanel.java    # Multiplayer rendering
```

## Security Notes

**WARNING**: This implementation is for local network play only.

For internet deployment, add:
- Encryption (SSL/TLS)
- Authentication
- Input validation
- Rate limiting
- Anti-cheat measures
- Player identity verification

## License

Same as the original Submarine Survival project.

---

**Enjoy cooperative submarine gameplay!** 🚢⚓
