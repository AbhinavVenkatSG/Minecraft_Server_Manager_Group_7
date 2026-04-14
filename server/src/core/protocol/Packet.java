package core.protocol;

import core.util.CRC16;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Packet {
    private final PacketType type;
    private final short crc;
    private final short length;
    private final String payload;

    public Packet(PacketType type, String payload) {
        byte[] payloadBytes = payload == null ? new byte[0] : payload.getBytes(StandardCharsets.UTF_8);
        this.type = type;
        this.payload = payload == null ? "" : payload;
        this.length = (short) payloadBytes.length;
        this.crc = CRC16.calculate(payloadBytes);
    }

    private Packet(PacketType type, short crc, short length, String payload) {
        this.type = type;
        this.crc = crc;
        this.length = length;
        this.payload = payload;
    }

    public byte[] toBytes() {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(5 + payloadBytes.length);
        buffer.put(type.getValue());
        buffer.putShort(crc);
        buffer.putShort(length);
        buffer.put(payloadBytes);
        return buffer.array();
    }

    public static Packet fromBytes(byte[] data) {
        if (data.length < 5) {
            throw new IllegalArgumentException("Packet is too short.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        PacketType type = PacketType.fromValue(buffer.get());
        short crc = buffer.getShort();
        short length = buffer.getShort();
        byte[] payloadBytes = new byte[length];
        buffer.get(payloadBytes);

        return new Packet(type, crc, length, new String(payloadBytes, StandardCharsets.UTF_8));
    }

    public boolean isValid() {
        return CRC16.verify(payload.getBytes(StandardCharsets.UTF_8), crc);
    }

    public PacketType getType() {
        return type;
    }

    public short getCrc() {
        return crc;
    }

    public short getLength() {
        return length;
    }

    public String getPayload() {
        return payload;
    }
}
