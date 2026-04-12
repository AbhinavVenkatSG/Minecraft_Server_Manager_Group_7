package com.minecraftmanager.websocket.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Packet {
    private PacketType type;
    private short crc;
    private short length;
    private String payload;

    public Packet() {}

    public Packet(PacketType type, String payload) {
        this.type = type;
        this.payload = payload;
        this.length = (short) (payload != null ? payload.getBytes(StandardCharsets.UTF_8).length : 0);
    }

    public Packet(PacketType type, short crc, short length, String payload) {
        this.type = type;
        this.crc = crc;
        this.length = length;
        this.payload = payload;
    }

    public byte[] toBytes() {
        byte[] payloadBytes = payload != null ? payload.getBytes(StandardCharsets.UTF_8) : new byte[0];
        this.crc = com.minecraftmanager.websocket.util.CRC16.calculate(payloadBytes);
        this.length = (short) payloadBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(5 + payloadBytes.length);
        buffer.put(type.getValue());
        buffer.putShort(crc);
        buffer.putShort(length);
        buffer.put(payloadBytes);

        return buffer.array();
    }

    public static Packet fromBytes(byte[] data) {
        if (data.length < 5) {
            throw new IllegalArgumentException("Invalid packet: too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte typeValue = buffer.get();
        short crc = buffer.getShort();
        short length = buffer.getShort();

        byte[] payloadBytes = new byte[length];
        buffer.get(payloadBytes);
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);

        return new Packet(PacketType.fromValue(typeValue), crc, length, payload);
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public short getCrc() {
        return crc;
    }

    public void setCrc(short crc) {
        this.crc = crc;
    }

    public short getLength() {
        return length;
    }

    public void setLength(short length) {
        this.length = length;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public boolean isValid() {
        if (payload == null) return false;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        return com.minecraftmanager.websocket.util.CRC16.verify(payloadBytes, crc);
    }
}