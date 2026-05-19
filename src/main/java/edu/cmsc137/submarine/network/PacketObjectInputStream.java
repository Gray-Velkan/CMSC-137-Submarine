package edu.cmsc137.submarine.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

final class PacketObjectInputStream extends ObjectInputStream {
    PacketObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        try {
            return super.resolveClass(desc);
        } catch (ClassNotFoundException ex) {
            Class<?> legacyClass = resolveLegacyPacketClass(desc.getName());
            if (legacyClass != null) {
                return legacyClass;
            }
            throw ex;
        }
    }

    private Class<?> resolveLegacyPacketClass(String className) {
        return switch (className) {
            case "ClientHelloPacket" -> ClientHelloPacket.class;
            case "GameStartPacket" -> GameStartPacket.class;
            case "LobbyStatePacket" -> LobbyStatePacket.class;
            case "StartGameRequestPacket" -> StartGameRequestPacket.class;
            case "WelcomePacket" -> WelcomePacket.class;
            default -> null;
        };
    }
}