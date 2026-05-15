package edu.cmsc137.submarine.core;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlayer {

    private Clip clip;
    private FloatControl volumeControl;
    private Timer fadeTimer;

    public AudioPlayer(String filePath) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            clip = AudioSystem.getClip();
            clip.open(audioStream);
            
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading audio file: " + filePath);
            e.printStackTrace();
        }
    }

    private void setVolume(float linearVolume) {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            if (linearVolume <= 0.001f) {
                volumeControl.setValue(min);
            } else {
                float db = (float) (20.0 * Math.log10(linearVolume));
                volumeControl.setValue(Math.max(min, Math.min(volumeControl.getMaximum(), db)));
            }
        }
    }

    public void playLooping(long startMicroseconds) {
        playLooping(startMicroseconds, 0);
    }

    public void playLooping(long startMicroseconds, int fadeMs) {
        if (clip != null) {
            if (fadeTimer != null) {
                fadeTimer.cancel();
                fadeTimer = null;
            }

            if (clip.isRunning() && fadeMs == 0) {
                clip.stop();
            } else if (!clip.isRunning()) {
                clip.setMicrosecondPosition(startMicroseconds);
            }
            
            if (fadeMs > 0 && volumeControl != null) {
                setVolume(0.01f);
                if (!clip.isRunning()) {
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                }
                
                fadeTimer = new Timer();
                final int steps = 20;
                final long delay = fadeMs / steps;
                
                fadeTimer.scheduleAtFixedRate(new TimerTask() {
                    int currentStep = 0;
                    @Override
                    public void run() {
                        currentStep++;
                        float vol = (float) currentStep / steps;
                        setVolume(vol);
                        if (currentStep >= steps) {
                            setVolume(1.0f);
                            this.cancel();
                        }
                    }
                }, delay, delay);
            } else {
                setVolume(1.0f);
                if (!clip.isRunning()) {
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                }
            }
        }
    }

    public void stop() {
        stop(0);
    }

    public void stop(int fadeMs) {
        if (clip != null && clip.isRunning()) {
            if (fadeTimer != null) {
                fadeTimer.cancel();
                fadeTimer = null;
            }

            if (fadeMs > 0 && volumeControl != null) {
                fadeTimer = new Timer();
                final int steps = 20;
                final long delay = fadeMs / steps;
                
                fadeTimer.scheduleAtFixedRate(new TimerTask() {
                    int currentStep = steps;
                    @Override
                    public void run() {
                        currentStep--;
                        float vol = (float) currentStep / steps;
                        setVolume(vol);
                        if (currentStep <= 0) {
                            clip.stop();
                            this.cancel();
                        }
                    }
                }, delay, delay);
            } else {
                clip.stop();
            }
        }
    }
}
