package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.core.AudioPlayer;
import edu.cmsc137.submarine.core.GameState;
import edu.cmsc137.submarine.core.ItemEntity;
import edu.cmsc137.submarine.core.ItemType;
import edu.cmsc137.submarine.core.TaskStation;
import edu.cmsc137.submarine.input.InputHandler;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {
    public static final int PANEL_WIDTH = 1280;
    public static final int PANEL_HEIGHT = 768;

    private static final int PLAYER_WIDTH = 26;
    private static final int PLAYER_HEIGHT = 26;
    private static final double PLAYER_SPEED_PX_PER_SEC = 190.0;

    private static final int TASK_STATION_W = 72;
    private static final int TASK_STATION_H = 72;
    private static final double TASK_INTERACTION_RADIUS = 78.0;

    private static final int ITEM_SIZE = 90;
    private static final int ITEM_DRAW_SIZE = 52;

    private static final double ITEM_PICKUP_RADIUS = 52.0;
    private static final ItemType NO_ITEM = ItemType.NONE;

    private static final double INITIAL_TIME_SECONDS = 180.0;
    private static final int TARGET_FPS = 60;
    private static final int HUD_VERTICAL_OFFSET = -40; //Input the destination of the hub

    protected final TileManager tileManager;
    protected GameState gameState;
    private final InputHandler inputHandler;
    protected final List<TaskStation> taskStations;
    protected final List<ItemEntity> worldItems;

    private Thread gameThread;
    private volatile boolean running;

    public enum ScreenState {
        TITLE, MAIN_MENU, SETTINGS, LOADING, GAME, PAUSED
    }

    private Image titleBgImage;
    private Image enterBtnImage;
    private Image enterHoverImage;
    private Image mainMenuBgImage;
    private Image playBtnImage;
    private Image playHoverImage;
    private Image settingsBtnImage;
    private Image settingsHoverImage;
    private Image settingsPageBgImage;
    private Image backBtnImage;
    private Image musicOnBtnImage;
    private Image musicOffBtnImage;
    private Image sfxOnBtnImage;
    private Image sfxOffBtnImage;
    private Image rulesBtnImage;
    private Image mapBgImage;
    private final Image loadingGifImage;
    private Image navbarImage;
    private Image pauseBtnImage;
    private Image pauseOverlayImage;
    private Image resumeBtnImage;
    private Image restartBtnImage;
    private Image quitBtnImage;
    protected AudioPlayer menuMusic;
    protected AudioPlayer gameMusic;
    private double loadingTimer = 0.0;
    protected boolean isMusicOn = true;
    private boolean isSfxOn = true;
    protected ScreenState currentScreen = ScreenState.TITLE;
    private ActionListener onExit;

    public void setOnExit(ActionListener listener) {
        this.onExit = listener;
    }

    private boolean isEnterHovered = false;
    private boolean isPlayHovered = false;
    private boolean isSettingsHovered = false;
    private Image wrenchImage;
    private Image welderImage;
    private Image handPumpImage;
    private Image patchPlateImage;
    private Image extinguisherImage;
    private Image defeatImage;
    private Image playagain;
    private Image quit;
    private Image victoryImage;

    public GamePanel() {
        this(null, null);
    }

    public GamePanel(edu.cmsc137.submarine.network.GameClient client, String localPlayerId) {
        this.tileManager = new TileManager();
        this.gameState = new GameState(
                tileManager.getMapWidthPixels(),
                tileManager.getMapHeightPixels(),
                INITIAL_TIME_SECONDS);
        this.inputHandler = new InputHandler();
        this.taskStations = createTaskStations();
        this.worldItems = createInitialItems();
        this.client = client;
        this.localPlayerId = localPlayerId;

        try {
            this.titleBgImage = ImageIO.read(new File("assets/title page.png"));
            this.enterBtnImage = ImageIO.read(new File("assets/enter.png"));
            this.enterHoverImage = ImageIO.read(new File("assets/enter_hover.png"));
            this.mainMenuBgImage = ImageIO.read(new File("assets/main menu.png"));
            this.playBtnImage = ImageIO.read(new File("assets/play.png"));
            this.playHoverImage = ImageIO.read(new File("assets/play_hover.png"));
            this.settingsBtnImage = ImageIO.read(new File("assets/settings.png"));
            this.settingsHoverImage = ImageIO.read(new File("assets/settings_hover.png"));
            this.settingsPageBgImage = ImageIO.read(new File("assets/settings_page.png"));
            this.backBtnImage = ImageIO.read(new File("assets/back_button1.png"));
            this.musicOnBtnImage = ImageIO.read(new File("assets/music_on.png"));
            this.musicOffBtnImage = ImageIO.read(new File("assets/music_off.png"));
                this.sfxOnBtnImage = ImageIO.read(new File("assets/sfx_on.png"));
                this.sfxOffBtnImage = ImageIO.read(new File("assets/sfx_off.png"));
                this.rulesBtnImage = ImageIO.read(new File("assets/rules.png"));
                this.mapBgImage = ImageIO.read(new File("assets/map.png"));
                this.wrenchImage      = ImageIO.read(new File("assets/wrench.png"));
                this.defeatImage = ImageIO.read(new File("assets/defeat.png"));

                this.playagain = ImageIO.read(new File("assets/playagain_button.png"));
                this.quit = ImageIO.read(new File("assets/quit.png"));

                this.victoryImage = ImageIO.read(new File("assets/victory.png"));

                this.welderImage      = ImageIO.read(new File("assets/welder.png"));
            this.handPumpImage    = ImageIO.read(new File("assets/handpump.png"));
            this.patchPlateImage  = ImageIO.read(new File("assets/plate.png"));
            this.extinguisherImage = ImageIO.read(new File("assets/extinguisher.png"));
            } catch (IOException e) {
                System.err.println("Failed to load images");
                e.printStackTrace();
            }

            // Use ImageIcon to support animated GIF
            this.loadingGifImage = new javax.swing.ImageIcon("assets/loading.gif").getImage();
            // Use ImageIcon for navbar to avoid ImageIO issues with specific PNG formats
            this.navbarImage = new javax.swing.ImageIcon("assets/navbar.png").getImage();
            this.pauseBtnImage = new javax.swing.ImageIcon("assets/pause_button.png").getImage();
            this.pauseOverlayImage = new javax.swing.ImageIcon("assets/pause.png").getImage();
            this.resumeBtnImage = new javax.swing.ImageIcon("assets/resume.png").getImage();
            this.restartBtnImage = new javax.swing.ImageIcon("assets/restart.png").getImage();
            this.quitBtnImage = new javax.swing.ImageIcon("assets/quit.png").getImage();

            this.menuMusic = new AudioPlayer("sound/menu_bgmusic.wav");
            this.gameMusic = new AudioPlayer("sound/gameplay_bgmusic.wav");
            if (isMusicOn) {
                menuMusic.playLooping(4_000_000L); // start from 4 seconds
            }

            addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    double scaleX = (double) getWidth() / PANEL_WIDTH;
                    double scaleY = (double) getHeight() / PANEL_HEIGHT;
                    double logicalX = e.getX() / scaleX;
                    double logicalY = e.getY() / scaleY;

                    if (currentScreen == ScreenState.TITLE) {
                        if (logicalX >= 464.55 && logicalX <= (464.55 + 350.9) &&
                                logicalY >= 515.8 && logicalY <= (515.8 + 145.5)) {
                            currentScreen = ScreenState.MAIN_MENU;
                        }
                    } else if (currentScreen == ScreenState.MAIN_MENU) {
                        // Play button tight text bounds
                        if (logicalX >= 98 && logicalX <= 379 &&
                                logicalY >= 410 && logicalY <= 488) {
                            resetGame();
                            currentScreen = ScreenState.LOADING;
                            loadingTimer = 0.0;
                        }
                        // Settings button tight text bounds
                        else if (logicalX >= 98 && logicalX <= 624 &&
                                logicalY >= 529 && logicalY <= 607) {
                            currentScreen = ScreenState.SETTINGS;
                        }
                    } else if (currentScreen == ScreenState.SETTINGS) {
                        if (logicalX >= 220.6 && logicalX <= (220.6 + 84.7) &&
                                logicalY >= 132.7 && logicalY <= (132.7 + 84.7)) {
                            currentScreen = ScreenState.MAIN_MENU;
                        }

                        if (logicalX >= 356.4 && logicalX <= (356.4 + 123.3) &&
                                logicalY >= 245.6 && logicalY <= (245.6 + 93.4)) {
                            isMusicOn = !isMusicOn;
                            if (isMusicOn) {
                                menuMusic.playLooping(4_000_000L);
                            } else {
                                menuMusic.stop();
                            }
                            repaint();
                        }

                        if (logicalX >= 578.3 && logicalX <= (578.3 + 123.3) &&
                                logicalY >= 247.4 && logicalY <= (247.4 + 93.4)) {
                            isSfxOn = !isSfxOn;
                            repaint();
                        }
                    } else if (currentScreen == ScreenState.GAME) {
                        if (gameState.isRoundOver()) {
                            // Any click on round-end overlay returns to title
                            currentScreen = ScreenState.MAIN_MENU;
                            gameMusic.stop();
                            if (isMusicOn) menuMusic.playLooping(4_000_000L);
                            repaint();
                            if (onExit != null) onExit.actionPerformed(null);
                        } else if (logicalX >= 1113.9 && logicalX <= (1113.9 + 84.9) &&
                                logicalY >= 54.5 && logicalY <= (54.5 + 84.9)) {
                            currentScreen = ScreenState.PAUSED;
                            repaint();
                        }

                        if (gameState.isRoundOver()) {
                            // Play Again button
                            if (logicalX >= 400 && logicalX <= (400 + 300) &&
                                    logicalY >= 450 && logicalY <= (450 + 80)) {
                                resetGame();
                                currentScreen = ScreenState.LOADING;
                                loadingTimer = 0.0;
                                gameMusic.stop();
                                if (isMusicOn) menuMusic.playLooping(4_000_000L);
                                repaint();
                            }
                            // Quit button
                            else if (logicalX >= 400 && logicalX <= (400 + 300) &&
                                    logicalY >= 550 && logicalY <= (550 + 80)) {
                                currentScreen = ScreenState.MAIN_MENU;
                                gameMusic.stop();
                                if (isMusicOn) menuMusic.playLooping(4_000_000L);
                                repaint();
                            }
                        }

                    } else if (currentScreen == ScreenState.PAUSED) {
                        // Resume
                        if (logicalX >= 447.8 && logicalX <= (447.8 + 384.4) &&
                                logicalY >= 340.5 && logicalY <= (340.5 + 83.2)) {
                            currentScreen = ScreenState.GAME;
                            repaint();
                        }
                        // Restart
                        else if (logicalX >= 447.8 && logicalX <= (447.8 + 384.4) &&
                                logicalY >= 456.2 && logicalY <= (456.2 + 83.2)) {
                            resetGame();
                            currentScreen = ScreenState.LOADING;
                            loadingTimer = 0.0;
                            gameMusic.stop();
                            if (isMusicOn) menuMusic.playLooping(4_000_000L);
                            repaint();
                        }
                        // Quit
                        else if (logicalX >= 447.8 && logicalX <= (447.8 + 384.4) &&
                                logicalY >= 565.9 && logicalY <= (565.9 + 83.2)) {
                            currentScreen = ScreenState.MAIN_MENU;
                            gameMusic.stop();
                            if (isMusicOn) menuMusic.playLooping(4_000_000L);
                            repaint();
                            if (onExit != null) onExit.actionPerformed(null);
                        }
                        // Music ON/OFF
                        else if (logicalX >= 467.4 && logicalX <= (467.4 + 123.3) &&
                                logicalY >= 217.3 && logicalY <= (217.3 + 93.4)) {
                            isMusicOn = !isMusicOn;
                            if (isMusicOn) {
                                gameMusic.playLooping(0);
                            } else {
                                gameMusic.stop();
                            }
                            repaint();
                        }
                        // SFX ON/OFF
                        else if (logicalX >= 689.3 && logicalX <= (689.3 + 123.3) &&
                                logicalY >= 219.1 && logicalY <= (219.1 + 93.4)) {
                            isSfxOn = !isSfxOn;
                            repaint();
                        }
                    }
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double scaleX = (double) getWidth() / PANEL_WIDTH;
                double scaleY = (double) getHeight() / PANEL_HEIGHT;
                double logicalX = e.getX() / scaleX;
                double logicalY = e.getY() / scaleY;

                if (currentScreen == ScreenState.TITLE) {
                    boolean hovered = logicalX >= 464.55 && logicalX <= (464.55 + 350.9) &&
                            logicalY >= 515.8 && logicalY <= (515.8 + 145.5);

                    if (hovered != isEnterHovered) {
                        isEnterHovered = hovered;
                        repaint();
                    }
                } else if (currentScreen == ScreenState.MAIN_MENU) {
                    boolean playHovered = logicalX >= 98 && logicalX <= 379 &&
                            logicalY >= 410 && logicalY <= 488;
                    boolean settingsHovered = logicalX >= 98 && logicalX <= 624 &&
                            logicalY >= 529 && logicalY <= 607;

                    if (playHovered != isPlayHovered || settingsHovered != isSettingsHovered) {
                        isPlayHovered = playHovered;
                        isSettingsHovered = settingsHovered;
                        repaint();
                    }
                }
            }
        });

        // place player on central hallway floor tile
        gameState.setPlayerPosition(tileToPixel(12) + 4, tileToPixel(13) + 3);

        // Set the window size smaller (1024x614) while keeping the 1280x768 logical
        // resolution
        setPreferredSize(new Dimension(1024, 614));
        setBackground(new Color(7, 14, 22));
        setFocusable(true);
        addKeyListener(inputHandler);

        // start loop once panel is attached to a visible window
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                startGameLoop();
                requestFocusInWindow();
            }
        });
    }

    private void resetGame() {
        this.gameState = new GameState(
                tileManager.getMapWidthPixels(),
                tileManager.getMapHeightPixels(),
                INITIAL_TIME_SECONDS);
        this.worldItems.clear();
        this.worldItems.addAll(createInitialItems());
        this.taskStations.clear();
        this.taskStations.addAll(createTaskStations());
        gameState.setPlayerPosition(tileToPixel(12) + 4, tileToPixel(13) + 3);
        inputHandler.consumeInteract();
    }

    public GameState getGameState() {
        return gameState;
    }

    public void startGameLoop() {
        if (running) {
            return;
        }
        running = true;
        gameThread = new Thread(this, "game-loop-thread");
        gameThread.start();
    }

    public void stopGameLoop() {
        running = false;
    }

    @Override
    public void run() {
        // fixed update step keeps simulation stable for future server sync
        final double fixedDeltaSeconds = 1.0 / TARGET_FPS;
        // target frame pacing for render loop
        final long nanosPerFrame = 1_000_000_000L / TARGET_FPS;

        long previousTime = System.nanoTime();
        double accumulator = 0.0;

        while (running) {
            long currentTime = System.nanoTime();
            long elapsedNanos = currentTime - previousTime;
            previousTime = currentTime;

            // accumulate elapsed time and consume it in fixed-size updates
            accumulator += elapsedNanos / 1_000_000_000.0;

            while (accumulator >= fixedDeltaSeconds) {
                if (currentScreen == ScreenState.LOADING) {
                    loadingTimer += fixedDeltaSeconds;
                    if (loadingTimer >= 5.0) {
                        currentScreen = ScreenState.GAME;
                        menuMusic.stop();
                        if (isMusicOn) gameMusic.playLooping(0);
                    }
                    inputHandler.consumeInteract(); // Clear queued input
                } else if (currentScreen != ScreenState.GAME) {
                    inputHandler.consumeInteract(); // Clear queued input
                } else {
                    processInput(fixedDeltaSeconds);
                    updateState(fixedDeltaSeconds);
                }
                accumulator -= fixedDeltaSeconds;
            }

                // periodically send local player state to server (20Hz target)
                if (client != null && localPlayerId != null) {
                    int count = sendCounter.incrementAndGet();
                    if (count % 3 == 0) { // approx every 3 frames at 60fps
                        try {
                            double px = gameState.getPlayerX();
                            double py = gameState.getPlayerY();
                            int fx = gameState.getPlayer().getFacingX();
                            int fy = gameState.getPlayer().getFacingY();
                            client.send(new edu.cmsc137.submarine.network.PlayerStatePacket(localPlayerId, px, py, fx, fy));
                        } catch (Exception ignored) {
                        }
                    }
                }

            repaint();

            // basic frame pacing
            long frameTime = System.nanoTime() - currentTime;
            long sleepNanos = nanosPerFrame - frameTime;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
    }

    protected InputHandler getInputHandler() {
        return inputHandler;
    }

    protected void updateState(double deltaSeconds) {
        // keep all simulation progression inside game state
        gameState.update(deltaSeconds);
    }

    protected void processInput(double deltaSeconds) {
        if (gameState.isRoundOver()) {
            return;
        }

        double dx = 0.0;
        double dy = 0.0;
        // convert keyboard state into movement intent
        if (inputHandler.isUpPressed()) {
            dy -= 1.0;
        }
        if (inputHandler.isDownPressed()) {
            dy += 1.0;
        }
        if (inputHandler.isLeftPressed()) {
            dx -= 1.0;
        }
        if (inputHandler.isRightPressed()) {
            dx += 1.0;
        }

        // normalize diagonal movement
        if (dx != 0.0 || dy != 0.0) {
            double length = Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
            // update facing direction based on wasd input
            updatePlayerFacing((int) Math.round(dx), (int) Math.round(dy));
        }

        double moveX = dx * PLAYER_SPEED_PX_PER_SEC * deltaSeconds;
        double moveY = dy * PLAYER_SPEED_PX_PER_SEC * deltaSeconds;
        movePlayerWithTileCollision(moveX, moveY);

        // drop held item to current position
        if (inputHandler.consumeDrop()) {
            dropHeldItemAtPlayer();
        }

        // throw held item ahead in facing direction
        if (inputHandler.consumeThrow()) {
            throwHeldItemAhead();
        }

        // interact tries pickup first, then station use
        if (inputHandler.consumeInteract()) {
            handleInteraction();
        }
    }

    private void updatePlayerFacing(int facingX, int facingY) {
        gameState.getPlayer().setFacingDirection(facingX, facingY);
    }

    private void movePlayerWithTileCollision(double moveX, double moveY) {
        // x-axis projection for AABB collision
        Rectangle projectedX = gameState.getProjectedPlayerHitbox(moveX, 0.0, PLAYER_WIDTH, PLAYER_HEIGHT);
        if (!tileManager.isSolidHitbox(projectedX)) {
            gameState.movePlayer(moveX, 0.0, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        // y-axis projection for AABB collision and wall sliding
        Rectangle projectedY = gameState.getProjectedPlayerHitbox(0.0, moveY, PLAYER_WIDTH, PLAYER_HEIGHT);
        if (!tileManager.isSolidHitbox(projectedY)) {
            gameState.movePlayer(0.0, moveY, PLAYER_WIDTH, PLAYER_HEIGHT);
        }
    }

    private void handleInteraction() {
        if (tryPickupNearbyItem()) {
            return;
        }
        tryUseNearbyTaskStation();
    }

    private boolean tryPickupNearbyItem() {
        if (isHoldingItem()) {
            return false;
        }

        int index = findNearbyItemIndex();
        if (index < 0) {
            return false;
        }

        ItemEntity pickedItem = worldItems.remove(index);
        gameState.setHeldItemType(pickedItem.getItemType());
        return true;
    }

    protected void tryUseNearbyTaskStation() {
        TaskStation station = findNearbyTaskStation();
        if (station == null) {
            return;
        }

        // Reactor Switch is a hold task, it doesn't give buoyancy directly on click.
        if (station.name.equals("Reactor Switch")) {
            return;
        }

        ItemType held = gameState.getHeldItemType();
        if (station.requiredItem == held) {
            // Check cooperative dependency: Hand Pump requires reactor switch to be active (isPowerRoutedToPump = true)
            if (station.name.equals("Reactor Core") && !gameState.isPowerRoutedToPump()) {
                // Fails: plays system beep error and prevents buoyancy reward
                java.awt.Toolkit.getDefaultToolkit().beep();
                System.out.println("Hand pump failed: No power routed from Reactor Switch! Hold E on Reactor Switch.");
                return;
            }

            // Attempt to interact (handles cooldown/anti-spam)
            if (station.interact()) {
                gameState.addBuoyancy(station.buoyancyReward);
            } else {
                // If on cooldown, play system beep and log it
                java.awt.Toolkit.getDefaultToolkit().beep();
                System.out.println("Task " + station.name + " is on cooldown!");
            }
        }
    }

    private void dropHeldItemAtPlayer() {
        if (!isHoldingItem()) {
            return;
        }

        int px = (int) Math.round(gameState.getPlayerX() + PLAYER_WIDTH * 0.5 - ITEM_SIZE * 0.5);
        int py = (int) Math.round(gameState.getPlayerY() + PLAYER_HEIGHT * 0.5 - ITEM_SIZE * 0.5);
        worldItems.add(new ItemEntity(px, py, gameState.getHeldItemType()));
        gameState.setHeldItemType(NO_ITEM);
    }

    private void throwHeldItemAhead() {
        if (!isHoldingItem()) {
            return;
        }

        // throw item using player's facing direction
        ItemEntity tossed = gameState.tossHeldItem();
        if (tossed != null) {
            worldItems.add(tossed);
        }
    }

    private boolean isHoldingItem() {
        return gameState.getHeldItemType() != NO_ITEM;
    }

    private int findNearbyItemIndex() {
        for (int i = 0; i < worldItems.size(); i++) {
            ItemEntity item = worldItems.get(i);
            double itemCenterX = item.getX() + ITEM_SIZE * 0.5;
            double itemCenterY = item.getY() + ITEM_SIZE * 0.5;
            if (gameState.isNearTaskStation(itemCenterX, itemCenterY, ITEM_PICKUP_RADIUS)) {
                return i;
            }
        }
        return -1;
    }

    private TaskStation findNearbyTaskStation() {
        for (TaskStation station : taskStations) {
            if (gameState.isNearTaskStation(station.centerX(), station.centerY(), TASK_INTERACTION_RADIUS)) {
                return station;
            }
        }
        return null;
    }

    private List<TaskStation> createTaskStations() {
        List<TaskStation> stations = new ArrayList<>();
        // command bridge - requires Wrench
        stations.add(new TaskStation("Nav Console", tileToPixel(5), tileToPixel(3), 18, ItemType.WRENCH));
        // reactor room - Hand Pump (requires power to be routed)
        stations.add(new TaskStation("Reactor Core", tileToPixel(22), tileToPixel(13), 20, ItemType.HAND_PUMP));
        // engine room - requires Welder
        stations.add(new TaskStation("Engine Console", tileToPixel(31), tileToPixel(8), 15, ItemType.WELDER));
        // storage room - Reactor Switch (for forced co-op dependency)
        stations.add(new TaskStation("Reactor Switch", tileToPixel(19), tileToPixel(4), 0, ItemType.NONE));
        return stations;
    }

    private static final int[][] VALID_SPAWN_TILES = {
        // Command bridge (far left)
        {3, 3}, {4, 3}, {5, 3}, {6, 3},
        {3, 4}, {4, 4}, {5, 4}, {6, 4},
    
        // Central hallway (spread out)
        {8, 13}, {10, 13}, {12, 13}, {14, 13}, {16, 13},
    
        // Storage / airlock room
        {19, 4}, {21, 4}, {23, 4},
    
        // Reactor room
        {20, 13}, {22, 13},
    
        // Engine room (far right)
        {29, 7}, {31, 7}, {33, 7},
        {29, 9}, {31, 9}, {33, 9},
    
        // Upper corridor / other rooms
        {8, 5}, {10, 5}, {12, 5},
        {15, 8}, {17, 8},
    };

    private List<ItemEntity> createInitialItems() {
        List<ItemEntity> items = new ArrayList<>();
    
        // All item types to spawn
        ItemType[] typesToSpawn = {
            ItemType.WRENCH,
            ItemType.PATCH_PLATE,
            ItemType.WELDER,
            ItemType.HAND_PUMP,
            ItemType.EXTINGUISHER
        };

        int[][] zone1 = {{3,3},{4,3},{5,3},{6,3},{3,4},{4,4},{5,4},{6,4}};        // command bridge
        int[][] zone2 = {{8,13},{10,13},{12,13},{14,13},{16,13}};                  // hallway
        int[][] zone3 = {{19,4},{21,4},{23,4},{19,3},{21,3},{23,3}};               // storage
        int[][] zone4 = {{20,13},{22,13},{20,14},{22,14}};                         // reactor
        int[][] zone5 = {{29,7},{31,7},{33,7},{29,9},{31,9},{33,9}};               // engine room
    
        int[][][] zones = {zone1, zone2, zone3, zone4, zone5};
    
        // Shuffle a copy of the valid spawn tiles
        List<ItemType> shuffledTypes = new ArrayList<>(List.of(typesToSpawn));
        java.util.Collections.shuffle(shuffledTypes);
    
        // Assign each item type a unique random tile
        for (int i = 0; i < zones.length; i++) {
            // Pick a random tile within the zone
            List<int[]> zoneTiles = new ArrayList<>(List.of(zones[i]));
            java.util.Collections.shuffle(zoneTiles);
            int[] tile = zoneTiles.get(0);
    
            items.add(new ItemEntity(tileToPixel(tile[0]), tileToPixel(tile[1]), shuffledTypes.get(i)));
        }
    
        return items;
    }

    private int tileToPixel(int tile) {
        return tile * TileManager.TILE_SIZE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Scale everything to fit the current panel size
        double scaleX = (double) getWidth() / PANEL_WIDTH;
        double scaleY = (double) getHeight() / PANEL_HEIGHT;
        g2.scale(scaleX, scaleY);

        if (currentScreen == ScreenState.TITLE) {
            if (titleBgImage != null) {
                g2.drawImage(titleBgImage, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, null);
            }
            if (enterBtnImage != null) {
                int btnX = (int) Math.round(464.55);
                int btnY = (int) Math.round(515.8);
                int btnW = (int) Math.round(350.9);
                int btnH = (int) Math.round(145.5);
                Image imgToDraw = (isEnterHovered && enterHoverImage != null) ? enterHoverImage : enterBtnImage;
                g2.drawImage(imgToDraw, btnX, btnY, btnW, btnH, null);
            }
            return;
        } else if (currentScreen == ScreenState.MAIN_MENU) {
            if (mainMenuBgImage != null) {
                g2.drawImage(mainMenuBgImage, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, null);
            }

            if (playBtnImage != null) {
                if (isPlayHovered && playHoverImage != null) {
                    g2.drawImage(playHoverImage, 62, 400, 418, 180, null);
                } else {
                    g2.drawImage(playBtnImage, 85, 390, 311, 115, null);
                }
            }

            if (settingsBtnImage != null) {
                if (isSettingsHovered && settingsHoverImage != null) {
                    g2.drawImage(settingsHoverImage, 94, 516, 607, 158, null);
                } else {
                    g2.drawImage(settingsBtnImage, 27, 505, 642, 131, null);
                }
            }

            return;
        } else if (currentScreen == ScreenState.SETTINGS) {
            if (settingsPageBgImage != null) {
                g2.drawImage(settingsPageBgImage, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, null);
            }
            if (backBtnImage != null) {
                g2.drawImage(backBtnImage, (int) Math.round(220.6), (int) Math.round(132.7), (int) Math.round(84.7),
                        (int) Math.round(84.7), null);
            }
            Image musicImg = isMusicOn ? musicOnBtnImage : musicOffBtnImage;
            if (musicImg != null) {
                g2.drawImage(musicImg, (int) Math.round(356.4), (int) Math.round(245.6), (int) Math.round(123.3),
                        (int) Math.round(93.4), null);
            }
            Image sfxImg = isSfxOn ? sfxOnBtnImage : sfxOffBtnImage;
            if (sfxImg != null) {
                g2.drawImage(sfxImg, (int) Math.round(578.3), (int) Math.round(247.4), (int) Math.round(123.3),
                        (int) Math.round(93.4), null);
            }
            if (rulesBtnImage != null) {
                g2.drawImage(rulesBtnImage, (int) Math.round(800.7), (int) Math.round(245.6), (int) Math.round(123.3),
                        (int) Math.round(93.4), null);
            }
            return;
        } else if (currentScreen == ScreenState.LOADING) {
            if (loadingGifImage != null) {
                // Pass 'this' as the ImageObserver so the GIF animates automatically
                g2.drawImage(loadingGifImage, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, this);
            } else {
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
                g2.setColor(Color.WHITE);
                g2.drawString("LOADING...", PANEL_WIDTH / 2 - 50, PANEL_HEIGHT / 2);
            }
            return;
        }

        if (mapBgImage != null) {
            g2.drawImage(mapBgImage, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, null);
        } else {
            drawSubmarineRoom(g2); // Fallback to tile manager if map is missing
        }
        drawTaskStations(g2);
        drawItems(g2);
        drawPlayer(g2);
        // drawTileDebug(g2);
        drawHud(g2);

        if (gameState.isRoundOver()) {
            drawRoundEndOverlay(g2);
        } else if (currentScreen == ScreenState.PAUSED) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
            if (pauseOverlayImage != null) {
                g2.drawImage(pauseOverlayImage, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, null);
            }
            
            if (resumeBtnImage != null) {
                g2.drawImage(resumeBtnImage, (int) Math.round(447.8), (int) Math.round(340.5), (int) Math.round(384.4), (int) Math.round(83.2), null);
            }
            if (restartBtnImage != null) {
                g2.drawImage(restartBtnImage, (int) Math.round(447.8), (int) Math.round(456.2), (int) Math.round(384.4), (int) Math.round(83.2), null);
            }
            if (quitBtnImage != null) {
                g2.drawImage(quitBtnImage, (int) Math.round(447.8), (int) Math.round(565.9), (int) Math.round(384.4), (int) Math.round(83.2), null);
            }
            
            Image musicImg = isMusicOn ? musicOnBtnImage : musicOffBtnImage;
            if (musicImg != null) {
                g2.drawImage(musicImg, (int) Math.round(467.4), (int) Math.round(217.3), (int) Math.round(123.3),
                        (int) Math.round(93.4), null);
            }
            Image sfxImg = isSfxOn ? sfxOnBtnImage : sfxOffBtnImage;
            if (sfxImg != null) {
                g2.drawImage(sfxImg, (int) Math.round(689.3), (int) Math.round(219.1), (int) Math.round(123.3),
                        (int) Math.round(93.4), null);
            }
        }

        // Note: do not call g2.dispose() here — Swing manages the Graphics lifecycle
        // and subclasses need the graphics context to remain valid for their own painting.
    }

    private void drawSubmarineRoom(Graphics2D g2) {
        tileManager.draw(g2);
    }

    private void drawTaskStations(Graphics2D g2) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        for (TaskStation station : taskStations) {
            boolean near = gameState.isNearTaskStation(station.centerX(), station.centerY(), TASK_INTERACTION_RADIUS);
            boolean interactable = station.isInteractable();
            
            // Link Hand Pump task dependency to power
            boolean powerIssue = station.name.equals("Reactor Core") && !gameState.isPowerRoutedToPump();
            
            Color base;
            if (!interactable) {
                // Cooldown: flashing red or dark gray
                long flashTime = System.currentTimeMillis() / 250 % 2;
                base = (flashTime == 0) ? new Color(180, 50, 50) : new Color(90, 90, 90);
            } else if (powerIssue) {
                // Dependency locked: steel blue color
                base = new Color(60, 80, 95);
            } else {
                base = near ? new Color(88, 192, 120) : new Color(208, 154, 84);
            }

            g2.setColor(base);
            g2.fill(new RoundRectangle2D.Double(
                    station.x, station.y, TASK_STATION_W, TASK_STATION_H, 12, 12));

            // Accent border
            g2.setColor(new Color(255, 255, 255, 40));
            g2.draw(new RoundRectangle2D.Double(
                    station.x, station.y, TASK_STATION_W, TASK_STATION_H, 12, 12));

            g2.setColor(Color.WHITE);
            // Draw text dynamically based on status
            if (!interactable) {
                g2.drawString("OVERHEATED", station.x + 4, station.y + 22);
                long timeLeft = (station.getLastUsedTime() + station.getCooldownDuration() - System.currentTimeMillis()) / 1000;
                g2.drawString(Math.max(0, timeLeft) + "s cooldown", station.x + 8, station.y + 40);
            } else if (powerIssue) {
                g2.drawString("NO POWER", station.x + 10, station.y + 22);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
                g2.drawString("ENABLE SWITCH", station.x + 4, station.y + 40);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            } else if (station.name.equals("Reactor Switch")) {
                g2.drawString("PWR SWITCH", station.x + 4, station.y + 22);
                boolean active = gameState.isPowerRoutedToPump();
                g2.drawString(active ? "[ACTIVE]" : "[OFFLINE]", station.x + 12, station.y + 40);
            } else {
                g2.drawString(station.name, station.x + 6, station.y + 22);
                g2.drawString(formatItemLabel(station.requiredItem), station.x + 8, station.y + 40);
            }
            
            // Draw rewards
            if (station.buoyancyReward > 0 && interactable && !powerIssue) {
                g2.setColor(new Color(150, 255, 150));
                g2.drawString("+" + station.buoyancyReward + " BUOY", station.x + 12, station.y + 58);
            }

            if (near) {
                g2.setColor(Color.YELLOW);
                if (station.name.equals("Reactor Switch")) {
                    g2.drawString("HOLD E", station.x + 15, station.y + TASK_STATION_H + 15);
                } else if (!interactable) {
                    g2.drawString("WAIT...", station.x + 18, station.y + TASK_STATION_H + 15);
                } else if (powerIssue) {
                    g2.drawString("NO POWER!", station.x + 8, station.y + TASK_STATION_H + 15);
                } else {
                    g2.drawString("Press E", station.x + 15, station.y + TASK_STATION_H + 15);
                }
            }
        }
    }

    private Image getItemImage(ItemType itemType) {
        return switch (itemType) {
            case WRENCH       -> wrenchImage;

            case WELDER       -> welderImage;
            case HAND_PUMP    -> handPumpImage;
            case PATCH_PLATE  -> patchPlateImage;
            case EXTINGUISHER -> extinguisherImage;
            default           -> null;
        };
    }

    private void drawItems(Graphics2D g2) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        for (ItemEntity item : worldItems) {
            boolean near = gameState.isNearTaskStation(
                    item.getX() + ITEM_SIZE * 0.5,
                    item.getY() + ITEM_SIZE * 0.5,
                    ITEM_PICKUP_RADIUS);
        int drawX = (int) Math.round(item.getX() + (ITEM_SIZE - ITEM_DRAW_SIZE) * 0.5);
        int drawY = (int) Math.round(item.getY() + (ITEM_SIZE - ITEM_DRAW_SIZE) * 0.5);

    
            Image img = getItemImage(item.getItemType());
            if (img != null) {
                g2.drawImage(img,
                    (int) Math.round(item.getX()),
                    (int) Math.round(item.getY()),
                    ITEM_SIZE, ITEM_SIZE, null);
            } else {
                // fallback colored circle if image missing
                // g2.setColor(near ? new Color(255, 240, 120) : getItemColor(item.getItemType()));
                g2.fillOval((int) Math.round(item.getX()), (int) Math.round(item.getY()), ITEM_SIZE, ITEM_SIZE);
                g2.setColor(new Color(32, 40, 50));
                g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.drawOval((int) Math.round(item.getX()), (int) Math.round(item.getY()), ITEM_SIZE, ITEM_SIZE);
            }
    
            // only show label and prompt when player is nearby
            if (near) {
                g2.setColor(Color.WHITE);
                g2.drawString(formatItemLabel(item.getItemType()),
                        (int) Math.round(item.getX()) - 4,
                        (int) Math.round(item.getY()) + ITEM_SIZE + 13);
                if (!isHoldingItem()) {
                    g2.setColor(new Color(200, 240, 200));
                    g2.drawString("Press E",
                            (int) Math.round(item.getX()) - 2,
                            (int) Math.round(item.getY()) + ITEM_SIZE + 26);
                }
            }
        }
    }
    // private Color getItemColor(ItemType itemType) {
    //     // give each item type a distinct color for visibility
    //     return switch (itemType) {
    //         case PATCH_PLATE -> new Color(220, 100, 100);
    //         case WELDER -> new Color(220, 150, 80);
    //         case HAND_PUMP -> new Color(100, 180, 220);
    //         case EXTINGUISHER -> new Color(200, 80, 220);
    //         case WRENCH -> new Color(150, 150, 150);
    //         case NONE -> new Color(100, 100, 100);
    //     };
    // }

    private void drawPlayer(Graphics2D g2) {
        int px = (int) Math.round(gameState.getPlayerX());
        int py = (int) Math.round(gameState.getPlayerY());

        g2.setColor(new Color(72, 176, 255));
        g2.fillRoundRect(px, py, PLAYER_WIDTH, PLAYER_HEIGHT, 8, 8);

        g2.setColor(new Color(14, 24, 34));
        g2.drawRoundRect(px, py, PLAYER_WIDTH, PLAYER_HEIGHT, 8, 8);
    }

    private void drawHud(Graphics2D g2) {
        if (navbarImage != null) {
            g2.drawImage(navbarImage, 0, HUD_VERTICAL_OFFSET, PANEL_WIDTH, PANEL_HEIGHT, null);
        }

        g2.setColor(new Color(240, 230, 140)); // A nice yellowish color for numbers
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 18));

        // Buoyancy: "48/100" format at bottom-left of Buoyancy box
        g2.drawString(gameState.getBuoyancy() + "/" + gameState.getBuoyancyTarget(), (int) Math.round(113.3),
                (int) Math.round(130.0 + HUD_VERTICAL_OFFSET));

        // Time: Format as whole number centered below the clock icon
        int totalSeconds = (int) Math.max(0, gameState.getTimeRemainingSeconds());
        g2.drawString(String.valueOf(totalSeconds), (int) Math.round(286.8), (int) Math.round(120.0 + HUD_VERTICAL_OFFSET));

        // Drain: Bottom-right below the pipe
        g2.drawString(String.format("%.1f/s", gameState.getBuoyancyDrainPerSecond()), (int) Math.round(357.0),
                (int) Math.round(130.0 + HUD_VERTICAL_OFFSET));

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 15));
        g2.drawString(formatItemLabel(gameState.getHeldItemType()).toUpperCase(), (int) Math.round(451.9),
                (int) Math.round(110.0 + HUD_VERTICAL_OFFSET));

        drawDepthGauge(g2, (int) Math.round(881.3), (int) Math.round(100.9), (int) Math.round(161.0),
                (int) Math.round(33.5));

        // Draw pause button
        if (pauseBtnImage != null) {
            g2.drawImage(pauseBtnImage, (int) Math.round(1113.9), (int) Math.round(54.5), (int) Math.round(84.9),
                    (int) Math.round(84.9), null);
        }
    }

    public void updateRemotePlayers(java.util.List<edu.cmsc137.submarine.network.PlayerSnapshot> players) {
        if (players == null) return;
        // replace map entries
        remotePlayers.clear();
        for (edu.cmsc137.submarine.network.PlayerSnapshot ps : players) {
            if (ps == null) continue;
            if (localPlayerId != null && localPlayerId.equals(ps.getPlayerId())) continue;
            remotePlayers.put(ps.getPlayerId(), ps);
        }
    }

    private void drawOtherPlayers(Graphics2D g2) {
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        for (edu.cmsc137.submarine.network.PlayerSnapshot ps : remotePlayers.values()) {
            int px = (int) Math.round(ps.getX());
            int py = (int) Math.round(ps.getY());
            g2.setColor(new Color(120, 200, 120));
            g2.fillRoundRect(px, py, PLAYER_WIDTH, PLAYER_HEIGHT, 8, 8);
            g2.setColor(new Color(12, 18, 26));
            g2.drawRoundRect(px, py, PLAYER_WIDTH, PLAYER_HEIGHT, 8, 8);
            g2.setColor(new Color(220, 230, 240));
            g2.drawString(ps.getPlayerName(), px, py - 6);
        }
    }

    private void drawDepthGauge(Graphics2D g2, int x, int y, int w, int h) {
        double ratio = Math.max(0.0, Math.min(1.0, gameState.getBuoyancy() / (double) gameState.getBuoyancyTarget()));
        int innerX = x + 8;
        int innerY = y + 8;
        int innerW = w - 16;
        int innerH = h - 16;

        g2.setColor(new Color(43, 58, 75));
        g2.fillRoundRect(innerX, innerY, innerW, innerH, 8, 8);

        int fillW = (int) Math.round(innerW * ratio);
        if (fillW > 0) {
            g2.setClip(innerX, innerY, fillW, innerH);
            g2.setPaint(new GradientPaint(innerX, innerY + innerH, new Color(27, 99, 168), innerX, innerY,
                    new Color(101, 208, 255)));
            g2.fillRoundRect(innerX, innerY, innerW, innerH, 8, 8);
            g2.setClip(null);
        }

        g2.setColor(new Color(226, 239, 250));
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 11));
        int depthMeters = (int) Math.round((1.0 - ratio) * 300.0);
        g2.drawString(depthMeters + " m", x + 10, y);
        g2.drawString("Surface", x + w - 58, y);
    }

    protected void drawRoundEndOverlay(Graphics2D g2) {
        if (gameState.hasWon()) {
            if (victoryImage != null) {
                g2.drawImage(victoryImage, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, null);
            } else {
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
                g2.drawString("MISSION SAVED", PANEL_WIDTH / 2 - 180, PANEL_HEIGHT / 2 - 10);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
                g2.drawString("Final Buoyancy: " + gameState.getBuoyancy(),
                        PANEL_WIDTH / 2 - 96, PANEL_HEIGHT / 2 + 28);
            }
        } else {
            if (defeatImage != null) {
                g2.drawImage(defeatImage, -80, 0, PANEL_WIDTH + 80, PANEL_HEIGHT, null);
            } else {
                // fallback text if image missing
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
                String title = gameState.hasLostBySinking() ? "SUBMARINE SUNK" : "TIME UP";
                g2.drawString(title, PANEL_WIDTH / 2 - 180, PANEL_HEIGHT / 2 - 10);
                if (playagain != null) {
                    g2.drawImage(playagain,
                        400, 450,  // x, y  ← adjust position
                        300, 80,   // w, h  ← adjust size
                        null);
                }
                if (quit != null) {
                    g2.drawImage(quit,
                        400, 550,  // x, y  ← adjust position
                        300, 80,   // w, h  ← adjust size
                        null);
                }
            }
        }

        // Draw "Return to Menu" button
        int buttonX = PANEL_WIDTH / 2 - 100;
        int buttonY = PANEL_HEIGHT / 2 + 80;
        int buttonW = 200;
        int buttonH = 60;

        g2.setColor(new Color(88, 192, 120));
        g2.fillRoundRect(buttonX, buttonY, buttonW, buttonH, 15, 15);

        g2.setColor(new Color(14, 24, 34));
        g2.setStroke(new java.awt.BasicStroke(2.0f));
        g2.drawRoundRect(buttonX, buttonY, buttonW, buttonH, 15, 15);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        String buttonText = "Return to Menu";
        int textX = buttonX + (buttonW - g2.getFontMetrics().stringWidth(buttonText)) / 2;
        int textY = buttonY + ((buttonH - g2.getFontMetrics().getHeight()) / 2) + g2.getFontMetrics().getAscent();
        g2.drawString(buttonText, textX, textY);
    }

    // private void drawTileDebug(Graphics2D g2) {
    //     int tileSize = TileManager.TILE_SIZE;
    //     g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
    
    //     for (int row = 0; row < tileManager.getMapHeightPixels() / tileSize; row++) {
    //         for (int col = 0; col < tileManager.getMapWidthPixels() / tileSize; col++) {
    //             int px = col * tileSize;
    //             int py = row * tileSize;
    
    //             // draw grid line
    //             g2.setColor(new Color(255, 255, 0, 60));
    //             g2.drawRect(px, py, tileSize, tileSize);
    //             g2.setColor(new Color(0, 0, 0, 120));
    //             g2.fillRect(px + 2, py + 2, 36, 16); 
    //             // draw tile coordinates
    //             g2.setColor(new Color(255, 255, 255, 180));
    //             g2.drawString(col + "," + row, px + 2, py + 10);
    //         }
    //     }
    
    //     // draw player tile position in corner for reference
    //     int playerTileX = (int) (gameState.getPlayerX() / tileSize);
    //     int playerTileY = (int) (gameState.getPlayerY() / tileSize);
    //     g2.setColor(Color.YELLOW);
    //     g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
    //     g2.drawString("Player tile: " + playerTileX + ", " + playerTileY, 20, 200);
    // }

    // Inner class removed because it is now defined in edu.cmsc137.submarine.core.TaskStation

    private String formatItemLabel(ItemType itemType) {
        if (itemType == null || itemType == ItemType.NONE) {
            return "none";
        }

        String raw = itemType.name().toLowerCase();
        String[] parts = raw.split("_");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(parts[i].charAt(0)).append(parts[i].substring(1));
        }
        return builder.toString();
    }

    public InputHandler getInputHandler() {
        return inputHandler;
    }
}
