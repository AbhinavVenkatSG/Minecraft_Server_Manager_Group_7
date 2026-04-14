package core.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketBuilderTest {

    @Test
    void buildCommand_withTextPayload_createsCorrectPacket() {
        Packet packet = PacketBuilder.buildCommand("stop");
        
        assertEquals(PacketType.COMMAND, packet.getType());
        assertEquals("stop", packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildResponse_withMessage_createsCorrectPacket() {
        Packet packet = PacketBuilder.buildResponse("Server started");
        
        assertEquals(PacketType.RESPONSE, packet.getType());
        assertEquals("Server started", packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildConsoleLog_withLogMessage_createsCorrectPacket() {
        Packet packet = PacketBuilder.buildConsoleLog("[INFO] Player joined");
        
        assertEquals(PacketType.CONSOLE_LOG, packet.getType());
        assertEquals("[INFO] Player joined", packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildHeartbeat_createsPacketWithPingPayload() {
        Packet packet = PacketBuilder.buildHeartbeat();
        
        assertEquals(PacketType.HEARTBEAT, packet.getType());
        assertEquals("ping", packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildFileChunk_withContent_createsCorrectPacket() {
        String content = "file content data";
        Packet packet = PacketBuilder.buildFileChunk(content);
        
        assertEquals(PacketType.FILE_CHUNK, packet.getType());
        assertEquals(content, packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildError_withErrorMessage_createsCorrectPacket() {
        Packet packet = PacketBuilder.buildError("Connection failed");
        
        assertEquals(PacketType.ERROR, packet.getType());
        assertEquals("Connection failed", packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildCommand_withEmptyString_createsValidPacket() {
        Packet packet = PacketBuilder.buildCommand("");
        
        assertEquals(PacketType.COMMAND, packet.getType());
        assertEquals("", packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildCommand_withSpecialCharacters_handlesCorrectly() {
        String special = "cmd with \"quotes\" and \\backslash\\ and\nnewline";
        Packet packet = PacketBuilder.buildCommand(special);
        
        assertEquals(PacketType.COMMAND, packet.getType());
        assertEquals(special, packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildCommand_withLongPayload_createsValidPacket() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("x");
        }
        String longPayload = sb.toString();
        Packet packet = PacketBuilder.buildCommand(longPayload);
        
        assertEquals(PacketType.COMMAND, packet.getType());
        assertEquals(longPayload, packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildResponse_withUnicodeCharacters_createsValidPacket() {
        String unicode = "Hello \u4e16\u754c \u00e9\u00e8\u00ea";
        Packet packet = PacketBuilder.buildResponse(unicode);
        
        assertEquals(PacketType.RESPONSE, packet.getType());
        assertEquals(unicode, packet.getPayload());
        assertTrue(packet.isValid());
    }

    @Test
    void buildHeartbeat_everyTimeCreatesNewInstance() {
        Packet packet1 = PacketBuilder.buildHeartbeat();
        Packet packet2 = PacketBuilder.buildHeartbeat();
        
        assertNotSame(packet1, packet2);
        assertEquals(packet1.getPayload(), packet2.getPayload());
    }

    @Test
    void allBuilderMethods_produceValidPackets() {
        Packet cmd = PacketBuilder.buildCommand("test");
        Packet resp = PacketBuilder.buildResponse("test");
        Packet log = PacketBuilder.buildConsoleLog("test");
        Packet hb = PacketBuilder.buildHeartbeat();
        Packet fc = PacketBuilder.buildFileChunk("test");
        Packet err = PacketBuilder.buildError("test");
        
        assertAll(
            () -> assertTrue(cmd.isValid()),
            () -> assertTrue(resp.isValid()),
            () -> assertTrue(log.isValid()),
            () -> assertTrue(hb.isValid()),
            () -> assertTrue(fc.isValid()),
            () -> assertTrue(err.isValid())
        );
    }
}
