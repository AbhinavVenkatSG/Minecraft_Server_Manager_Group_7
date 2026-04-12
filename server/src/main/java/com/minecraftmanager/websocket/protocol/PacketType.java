package com.minecraftmanager.websocket.protocol;

public enum PacketType {
    COMMAND((byte) 0x01, "Command from client"),
    RESPONSE((byte) 0x02, "Response from server"),
    CONSOLE_LOG((byte) 0x03, "Console output stream"),
    HEARTBEAT((byte) 0x04, "Keep-alive ping/pong"),
    FILE_CHUNK((byte) 0x05, "File transfer chunk"),
    ERROR((byte) 0x06, "Error message");

    private final byte value;
    private final String description;

    PacketType(byte value, String description) {
        this.value = value;
        this.description = description;
    }

    public byte getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static PacketType fromValue(byte value) {
        for (PacketType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown packet type: " + value);
    }
}