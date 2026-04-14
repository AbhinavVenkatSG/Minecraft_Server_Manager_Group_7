package core.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketTypeTest {

    @Test
    void COMMAND_getValue_returns0x01() {
        assertEquals((byte) 0x01, PacketType.COMMAND.getValue());
    }

    @Test
    void RESPONSE_getValue_returns0x02() {
        assertEquals((byte) 0x02, PacketType.RESPONSE.getValue());
    }

    @Test
    void CONSOLE_LOG_getValue_returns0x03() {
        assertEquals((byte) 0x03, PacketType.CONSOLE_LOG.getValue());
    }

    @Test
    void HEARTBEAT_getValue_returns0x04() {
        assertEquals((byte) 0x04, PacketType.HEARTBEAT.getValue());
    }

    @Test
    void FILE_CHUNK_getValue_returns0x05() {
        assertEquals((byte) 0x05, PacketType.FILE_CHUNK.getValue());
    }

    @Test
    void ERROR_getValue_returns0x06() {
        assertEquals((byte) 0x06, PacketType.ERROR.getValue());
    }

    @Test
    void TELEMETRY_getValue_returns0x07() {
        assertEquals((byte) 0x07, PacketType.TELEMETRY.getValue());
    }

    @Test
    void fromValue_withValidByte_returnsCorrectType() {
        assertEquals(PacketType.COMMAND, PacketType.fromValue((byte) 0x01));
        assertEquals(PacketType.RESPONSE, PacketType.fromValue((byte) 0x02));
        assertEquals(PacketType.CONSOLE_LOG, PacketType.fromValue((byte) 0x03));
        assertEquals(PacketType.HEARTBEAT, PacketType.fromValue((byte) 0x04));
        assertEquals(PacketType.FILE_CHUNK, PacketType.fromValue((byte) 0x05));
        assertEquals(PacketType.ERROR, PacketType.fromValue((byte) 0x06));
        assertEquals(PacketType.TELEMETRY, PacketType.fromValue((byte) 0x07));
    }

    @Test
    void fromValue_withInvalidByte_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromValue((byte) 0x00));
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromValue((byte) 0x08));
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromValue((byte) 0xFF));
    }

    @Test
    void fromValue_withNegativeByte_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromValue((byte) -1));
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromValue((byte) -128));
    }

    @Test
    void allEnumValues_haveUniqueBytes() {
        byte[] values = new byte[PacketType.values().length];
        for (PacketType type : PacketType.values()) {
            for (byte existing : values) {
                if (existing == type.getValue()) {
                    fail("Duplicate byte value found: " + existing);
                }
            }
            values[PacketType.values().length - 1] = type.getValue();
        }
    }
}
