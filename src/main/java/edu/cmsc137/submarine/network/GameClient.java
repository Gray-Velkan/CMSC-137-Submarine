package edu.cmsc137.submarine.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile PacketListener listener;

    public GameClient(PacketListener listener) {
        this.listener = listener;
    }

    public void setListener(PacketListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 3000);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new PacketObjectInputStream(socket.getInputStream());

        // start reader thread
        Thread reader = new Thread(this::readLoop, "client-reader-thread");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        try {
            while (!socket.isClosed()) {
                Object obj = in.readObject();
                if (listener != null) {
                    listener.onPacket(obj);
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            // Inform listener about disconnection using null packet? For now just log.
            System.err.println("GameClient readLoop ended: " + ex.getMessage());
        } finally {
            close();
        }
    }

    public void send(Object o) throws IOException {
        if (out == null) throw new IOException("not connected");
        out.writeObject(o);
        out.flush();
    }

    public void close() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
