package core.protocol;

public enum PacketType {
    COMMAND((byte) 0x01),
    RESPONSE((byte) 0x02),
    CONSOLE_LOG((byte) 0x03),
    HEARTBEAT((byte) 0x04),
    FILE_CHUNK((byte) 0x05),
    ERROR((byte) 0x06),
    TELEMETRY((byte) 0x07);

    private final byte value;

    PacketType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
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
