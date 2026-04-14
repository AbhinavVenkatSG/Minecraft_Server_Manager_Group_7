package core.protocol;

public final class PacketBuilder {
    private PacketBuilder() {
    }

    public static Packet buildCommand(String command) {
        return new Packet(PacketType.COMMAND, command);
    }

    public static Packet buildResponse(String message) {
        return new Packet(PacketType.RESPONSE, message);
    }

    public static Packet buildConsoleLog(String message) {
        return new Packet(PacketType.CONSOLE_LOG, message);
    }

    public static Packet buildHeartbeat() {
        return new Packet(PacketType.HEARTBEAT, "ping");
    }

    public static Packet buildFileChunk(String payload) {
        return new Packet(PacketType.FILE_CHUNK, payload);
    }

    public static Packet buildError(String message) {
        return new Packet(PacketType.ERROR, message);
    }
}
