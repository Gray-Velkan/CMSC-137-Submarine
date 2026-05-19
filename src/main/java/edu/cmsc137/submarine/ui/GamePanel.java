package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.core.AudioPlayer;
import edu.cmsc137.submarine.core.GameState;
import edu.cmsc137.submarine.core.ItemEntity;
import edu.cmsc137.submarine.core.ItemType;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
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

    private static final int ITEM_SIZE = 16;
    private static final double ITEM_PICKUP_RADIUS = 52.0;
    private static final ItemType NO_ITEM = ItemType.NONE;

    private static final double INITIAL_TIME_SECONDS = 180.0;
    private static final int TARGET_FPS = 60;

    private final TileManager tileManager;
    private GameState gameState;
    private final InputHandler inputHandler;
    private final List<TaskStation> taskStations;
    private final List<ItemEntity> worldItems;

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
    private AudioPlayer menuMusic;
    private AudioPlayer gameMusic;
    private double loadingTimer = 0.0;
    private boolean isMusicOn = true;
    private boolean isSfxOn = true;
    private ScreenState currentScreen = ScreenState.TITLE;
    private boolean isEnterHovered = false;
    private boolean isPlayHovered = false;
    private boolean isSettingsHovered = false;

    public GamePanel() {
        this.tileManager = new TileManager();
        this.gameState = new GameState(
                tileManager.getMapWidthPixels(),
                tileManager.getMapHeightPixels(),
                INITIAL_TIME_SECONDS);
        this.inputHandler = new InputHandler();
        this.taskStations = createTaskStations();
        this.worldItems = createInitialItems();

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
                        } else if (logicalX >= 1113.9 && logicalX <= (1113.9 + 84.9) &&
                                logicalY >= 54.5 && logicalY <= (54.5 + 84.9)) {
                            currentScreen = ScreenState.PAUSED;
                            repaint();
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

    private void tryUseNearbyTaskStation() {
        TaskStation station = findNearbyTaskStation();
        if (station == null) {
            return;
        }

        ItemType held = gameState.getHeldItemType();
        if (station.requiredItem == held) {
            // reward buoyancy but do not consume the item
            // player keeps the item and can use it again or drop it
            gameState.addBuoyancy(station.buoyancyReward);
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
        // command bridge
        stations.add(new TaskStation("Nav Console", tileToPixel(5), tileToPixel(3), 18, ItemType.WRENCH));
        // reactor room
        stations.add(new TaskStation("Reactor Core", tileToPixel(22), tileToPixel(13), 20, ItemType.HAND_PUMP));
        // engine room
        stations.add(new TaskStation("Engine Console", tileToPixel(31), tileToPixel(8), 15, ItemType.WELDER));
        return stations;
    }

    private List<ItemEntity> createInitialItems() {
        List<ItemEntity> items = new ArrayList<>();
        // storage / airlock room
        items.add(new ItemEntity(tileToPixel(20), tileToPixel(4), ItemType.WRENCH));
        items.add(new ItemEntity(tileToPixel(22), tileToPixel(4), ItemType.PATCH_PLATE));
        items.add(new ItemEntity(tileToPixel(24), tileToPixel(4), ItemType.WELDER));
        // reactor room
        items.add(new ItemEntity(tileToPixel(20), tileToPixel(14), ItemType.HAND_PUMP));
        items.add(new ItemEntity(tileToPixel(22), tileToPixel(14), ItemType.EXTINGUISHER));
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
            g2.dispose();
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

            g2.dispose();
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
            g2.dispose();
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
            g2.dispose();
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

        g2.dispose();
    }

    private void drawSubmarineRoom(Graphics2D g2) {
        tileManager.draw(g2);
    }

    private void drawTaskStations(Graphics2D g2) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        for (TaskStation station : taskStations) {
            boolean near = gameState.isNearTaskStation(station.centerX(), station.centerY(), TASK_INTERACTION_RADIUS);
            Color base = near ? new Color(88, 192, 120) : new Color(208, 154, 84);

            g2.setColor(base);
            g2.fill(new RoundRectangle2D.Double(
                    station.x, station.y, TASK_STATION_W, TASK_STATION_H, 12, 12));

            g2.setColor(new Color(12, 18, 26));
            g2.drawString("TASK", station.x + 22, station.y + 22);
            g2.drawString(formatItemLabel(station.requiredItem), station.x + 8, station.y + 40);
            g2.drawString("+" + station.buoyancyReward, station.x + 24, station.y + 56);

            if (near) {
                g2.drawString("Press E", station.x + 8, station.y + TASK_STATION_H + 15);
            }
        }
    }

    private void drawItems(Graphics2D g2) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        for (ItemEntity item : worldItems) {
            boolean near = gameState.isNearTaskStation(
                    item.getX() + ITEM_SIZE * 0.5,
                    item.getY() + ITEM_SIZE * 0.5,
                    ITEM_PICKUP_RADIUS);

            // draw item with bright highlight when nearby
            g2.setColor(near ? new Color(255, 240, 120) : getItemColor(item.getItemType()));
            g2.fillOval((int) Math.round(item.getX()), (int) Math.round(item.getY()), ITEM_SIZE, ITEM_SIZE);

            // add dark outline for visibility
            g2.setColor(new Color(32, 40, 50));
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.drawOval((int) Math.round(item.getX()), (int) Math.round(item.getY()), ITEM_SIZE, ITEM_SIZE);

            g2.setColor(new Color(16, 22, 30));
            g2.drawString(formatItemLabel(item.getItemType()), (int) Math.round(item.getX()) - 8,
                    (int) Math.round(item.getY()) - 4);

            if (near && !isHoldingItem()) {
                g2.drawString("Press E", (int) Math.round(item.getX()) - 2,
                        (int) Math.round(item.getY()) + ITEM_SIZE + 14);
            }
        }
    }

    private Color getItemColor(ItemType itemType) {
        // give each item type a distinct color for visibility
        return switch (itemType) {
            case PATCH_PLATE -> new Color(220, 100, 100);
            case WELDER -> new Color(220, 150, 80);
            case HAND_PUMP -> new Color(100, 180, 220);
            case EXTINGUISHER -> new Color(200, 80, 220);
            case WRENCH -> new Color(150, 150, 150);
            case NONE -> new Color(100, 100, 100);
        };
    }

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
            g2.drawImage(navbarImage, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, null);
        }

        g2.setColor(new Color(240, 230, 140)); // A nice yellowish color for numbers
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 18));

        // Buoyancy: "48/100" format at bottom-left of Buoyancy box
        g2.drawString(gameState.getBuoyancy() + "/" + gameState.getBuoyancyTarget(), (int) Math.round(113.3),
                (int) Math.round(130.0));

        // Time: Format as whole number centered below the clock icon
        int totalSeconds = (int) Math.max(0, gameState.getTimeRemainingSeconds());
        g2.drawString(String.valueOf(totalSeconds), (int) Math.round(286.8), (int) Math.round(120.0));

        // Drain: Bottom-right below the pipe
        g2.drawString(String.format("%.1f/s", gameState.getBuoyancyDrainPerSecond()), (int) Math.round(357.0),
                (int) Math.round(130.0));

        // Held Item: Centered below the wrench box
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 15));
        g2.drawString(formatItemLabel(gameState.getHeldItemType()).toUpperCase(), (int) Math.round(451.9),
                (int) Math.round(110.0));

        // Draw the depth gauge over the DEPTH box
        drawDepthGauge(g2, (int) Math.round(881.3), (int) Math.round(100.9), (int) Math.round(161.0),
                (int) Math.round(33.5));

        // Draw pause button
        if (pauseBtnImage != null) {
            // Sizing down to fit the navbar
            g2.drawImage(pauseBtnImage, (int) Math.round(1113.9), (int) Math.round(54.5), (int) Math.round(84.9),
                    (int) Math.round(84.9), null);
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

    private void drawRoundEndOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));

        String title;
        if (gameState.hasWon()) {
            title = "MISSION SAVED";
        } else if (gameState.hasLostBySinking()) {
            title = "SUBMARINE SUNK";
        } else {
            title = "TIME UP";
        }
        g2.drawString(title, PANEL_WIDTH / 2 - 180, PANEL_HEIGHT / 2 - 10);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        g2.drawString(
                "Final Buoyancy: " + gameState.getBuoyancy(),
                PANEL_WIDTH / 2 - 96,
                PANEL_HEIGHT / 2 + 28);

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

    private static final class TaskStation {
        private final String name;
        private final int x;
        private final int y;
        private final int buoyancyReward;
        private final ItemType requiredItem;

        private TaskStation(String name, int x, int y, int buoyancyReward, ItemType requiredItem) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.buoyancyReward = buoyancyReward;
            this.requiredItem = requiredItem == null ? ItemType.NONE : requiredItem;
        }

        private double centerX() {
            return x + TASK_STATION_W * 0.5;
        }

        private double centerY() {
            return y + TASK_STATION_H * 0.5;
        }
    }

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
}
