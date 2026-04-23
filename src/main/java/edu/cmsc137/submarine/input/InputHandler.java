package edu.cmsc137.submarine.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

// collects input intent only and does not mutate game state directly
public class InputHandler implements KeyListener {
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;

    // one-shot action consumed by the game loop
    private boolean interactQueued;
    private boolean dropQueued;

    @Override
    public void keyTyped(KeyEvent e) {
        // not used for this game
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // set key state when pressed
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> upPressed = true;
            case KeyEvent.VK_S -> downPressed = true;
            case KeyEvent.VK_A -> leftPressed = true;
            case KeyEvent.VK_D -> rightPressed = true;
            case KeyEvent.VK_E -> interactQueued = true;
            case KeyEvent.VK_Q -> dropQueued = true;
            default -> {
                // ignore unrelated keys
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // clear key state when released
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> upPressed = false;
            case KeyEvent.VK_S -> downPressed = false;
            case KeyEvent.VK_A -> leftPressed = false;
            case KeyEvent.VK_D -> rightPressed = false;
            default -> {
                // ignore unrelated keys
            }
        }
    }

    public boolean isUpPressed() {
        return upPressed;
    }

    public boolean isDownPressed() {
        return downPressed;
    }

    public boolean isLeftPressed() {
        return leftPressed;
    }

    public boolean isRightPressed() {
        return rightPressed;
    }

    public boolean consumeInteract() {
        // return true once, then clear pending interaction
        if (interactQueued) {
            interactQueued = false;
            return true;
        }
        return false;
    }

    public boolean consumeDrop() {
        // return true once, then clear pending drop
        if (dropQueued) {
            dropQueued = false;
            return true;
        }
        return false;
    }
}
