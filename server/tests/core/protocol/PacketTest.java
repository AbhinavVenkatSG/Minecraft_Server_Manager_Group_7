package core.protocol;

import core.util.CRC16;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class PacketTest {

    @Test
    void constructor_withValidInput_setsFieldsCorrectly() {
        Packet packet = new Packet(PacketType.COMMAND, "test");
        
        assertEquals(PacketType.COMMAND, packet.getType());
        assertEquals("test", packet.getPayload());
        assertEquals((short) 4, packet.getLength());
        assertNotNull(packet.getPayload());
    }

    @Test
    void constructor_withNullPayload_treatsAsEmptyString() {
        Packet packet = new Packet(PacketType.COMMAND, null);
        
        assertEquals("", packet.getPayload());
        assertEquals((short) 0, packet.getLength());
        assertTrue(packet.isValid());
    }

    @Test
    void constructor_calculatesCrcCorrectly() {
        Packet packet = new Packet(PacketType.RESPONSE, "hello");
        byte[] payloadBytes = "hello".getBytes(StandardCharsets.UTF_8);
        short expectedCrc = CRC16.calculate(payloadBytes);
        
        assertEquals(expectedCrc, packet.getCrc());
    }

    @Test
    void toBytes_producesCorrectByteArray() {
        Packet packet = new Packet(PacketType.COMMAND, "hi");
        byte[] bytes = packet.toBytes();
        
        assertEquals(5 + 2, bytes.length);
        assertEquals((byte) 0x01, bytes[0]);
        assertNotNull(Arrays.copyOfRange(bytes, 1, 3));
        assertNotNull(Arrays.copyOfRange(bytes, 3, 5));
    }

    @Test
    void toBytes_roundTrip_matchesOriginal() {
        Packet original = new Packet(PacketType.RESPONSE, "roundtrip test");
        byte[] bytes = original.toBytes();
        
        Packet restored = Packet.fromBytes(bytes);
        
        assertEquals(original.getType(), restored.getType());
        assertEquals(original.getPayload(), restored.getPayload());
        assertEquals(original.getCrc(), restored.getCrc());
        assertEquals(original.getLength(), restored.getLength());
    }

    @Test
    void fromBytes_withTooShortData_throwsException() {
        byte[] shortData = new byte[] { 0x01, 0x02, 0x03 };
        
        assertThrows(IllegalArgumentException.class, () -> Packet.fromBytes(shortData));
    }

    @Test
    void fromBytes_withInvalidPacketType_throwsException() {
        byte[] data = new byte[10];
        data[0] = 0x00;
        
        assertThrows(IllegalArgumentException.class, () -> Packet.fromBytes(data));
    }

    @Test
    void fromBytes_withValidData_restoresPacket() {
        Packet original = new Packet(PacketType.FILE_CHUNK, "file content");
        byte[] bytes = original.toBytes();
        
        Packet restored = Packet.fromBytes(bytes);
        
        assertEquals(original.getType(), restored.getType());
        assertEquals(original.getPayload(), restored.getPayload());
        assertEquals(original.getCrc(), restored.getCrc());
    }

    @Test
    void isValid_withMatchingCrc_returnsTrue() {
        Packet packet = new Packet(PacketType.HEARTBEAT, "ping");
        assertTrue(packet.isValid());
    }

    @Test
    void isValid_withTamperedPayload_returnsFalse() {
        Packet original = new Packet(PacketType.COMMAND, "original");
        byte[] bytes = original.toBytes();
        bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] + 1);
        
        Packet tampered = Packet.fromBytes(bytes);
        assertFalse(tampered.isValid());
    }

    @Test
    void getType_returnsCorrectType() {
        Packet packet1 = new Packet(PacketType.COMMAND, "test");
        Packet packet2 = new Packet(PacketType.RESPONSE, "test");
        Packet packet3 = new Packet(PacketType.ERROR, "test");
        
        assertEquals(PacketType.COMMAND, packet1.getType());
        assertEquals(PacketType.RESPONSE, packet2.getType());
        assertEquals(PacketType.ERROR, packet3.getType());
    }

    @Test
    void getCrc_returnsCalculatedCrc() {
        Packet packet = new Packet(PacketType.CONSOLE_LOG, "test");
        short crc = packet.getCrc();
        assertTrue(crc >= Short.MIN_VALUE && crc <= Short.MAX_VALUE);
    }

    @Test
    void getLength_returnsPayloadLength() {
        String payload = "test payload";
        Packet packet = new Packet(PacketType.CONSOLE_LOG, payload);
        
        assertEquals((short) payload.length(), packet.getLength());
    }

    @Test
    void getPayload_returnsExactPayload() {
        String payload = "exact payload content";
        Packet packet = new Packet(PacketType.FILE_CHUNK, payload);
        
        assertEquals(payload, packet.getPayload());
    }

    @Test
    void multipleRoundTrips_preserveData() {
        String[] payloads = { "", "a", "hello", "hello world", "special: \"\\n\r" };
        
        for (String payload : payloads) {
            Packet original = new Packet(PacketType.COMMAND, payload);
            Packet restored = Packet.fromBytes(original.toBytes());
            
            assertEquals(original.getType(), restored.getType());
            assertEquals(original.getPayload(), restored.getPayload());
            assertTrue(restored.isValid());
        }
    }

    @Test
    void emptyPayload_packetCreationAndValidation() {
        Packet packet = new Packet(PacketType.HEARTBEAT, "");
        
        assertEquals("", packet.getPayload());
        assertEquals((short) 0, packet.getLength());
        assertTrue(packet.isValid());
        
        byte[] bytes = packet.toBytes();
        Packet restored = Packet.fromBytes(bytes);
        
        assertEquals(packet.getPayload(), restored.getPayload());
    }

    @Test
    void largePayload_handlesCorrectly() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("x");
        }
        String largePayload = sb.toString();
        
        Packet packet = new Packet(PacketType.FILE_CHUNK, largePayload);
        byte[] bytes = packet.toBytes();
        Packet restored = Packet.fromBytes(bytes);
        
        assertEquals(largePayload, restored.getPayload());
        assertTrue(restored.isValid());
    }

    @Test
    void getPayload_withSpecialCharacters_preservesExactContent() {
        String special = "line1\nline2\rline3\ttab\"quote\\backslash";
        Packet packet = new Packet(PacketType.COMMAND, special);
        
        byte[] bytes = packet.toBytes();
        Packet restored = Packet.fromBytes(bytes);
        
        assertEquals(special, restored.getPayload());
        assertTrue(restored.isValid());
    }
}
