package com.minecraftmanager.websocket.protocol;

public class PacketBuilder {
    
    public static Packet build(PacketType type, String payload) {
        return new Packet(type, payload);
    }

    public static Packet buildCommand(String command) {
        return new Packet(PacketType.COMMAND, command);
    }

    public static Packet buildResponse(String message) {
        return new Packet(PacketType.RESPONSE, message);
    }

    public static Packet buildConsoleLog(String log) {
        return new Packet(PacketType.CONSOLE_LOG, log);
    }

    public static Packet buildHeartbeat() {
        return new Packet(PacketType.HEARTBEAT, "ping");
    }

    public static Packet buildFileChunk(byte[] chunkData, int chunkNumber, int totalChunks) {
        String payload = chunkNumber + "|" + totalChunks + "|" + new String(chunkData);
        return new Packet(PacketType.FILE_CHUNK, payload);
    }

    public static Packet buildError(String errorMessage) {
        return new Packet(PacketType.ERROR, errorMessage);
    }

    public static byte[] toBytes(Packet packet) {
        return packet.toBytes();
    }

    public static Packet fromBytes(byte[] data) {
        return Packet.fromBytes(data);
    }
}