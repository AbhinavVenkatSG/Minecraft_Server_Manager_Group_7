package core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CRC16Test {

    @Test
    void calculate_withEmptyArray_returnsPresetValue() {
        byte[] emptyData = new byte[0];
        short crc = CRC16.calculate(emptyData);
        assertEquals((short) 0xFFFF, crc);
    }

    @Test
    void calculate_withSingleByte_returnsConsistentCrc() {
        byte[] data = new byte[] { 0x01 };
        short crc = CRC16.calculate(data);
        assertTrue(CRC16.verify(data, crc));
        assertEquals(crc, CRC16.calculate(data));
    }

    @Test
    void calculate_withKnownVector_returnsExpectedValue() {
        byte[] data = "123456789".getBytes();
        short crc = CRC16.calculate(data);
        assertEquals((short) 0x29B1, crc);
    }

    @Test
    void calculate_withAllZeros_returnsConsistentCrc() {
        byte[] data = new byte[] { 0x00, 0x00, 0x00, 0x00 };
        short crc = CRC16.calculate(data);
        assertTrue(CRC16.verify(data, crc));
        assertEquals(crc, CRC16.calculate(data));
    }

    @Test
    void calculate_withAllFF_returnsCorrectValue() {
        byte[] data = new byte[] { (byte) 0xFF, (byte) 0xFF };
        short crc = CRC16.calculate(data);
        assertNotEquals((short) 0xFFFF, crc);
    }

    @Test
    void verify_withCorrectCrc_returnsTrue() {
        byte[] data = "test".getBytes();
        short crc = CRC16.calculate(data);
        assertTrue(CRC16.verify(data, crc));
    }

    @Test
    void verify_withIncorrectCrc_returnsFalse() {
        byte[] data = "test".getBytes();
        short wrongCrc = (short) 0x0000;
        assertFalse(CRC16.verify(data, wrongCrc));
    }

    @Test
    void calculate_andVerify_areConsistent() {
        byte[] data = "Hello, World!".getBytes();
        short crc = CRC16.calculate(data);
        assertTrue(CRC16.verify(data, crc));
        assertFalse(CRC16.verify(data, (short) (crc + 1)));
    }

    @Test
    void calculate_withLargeData_handlesCorrectly() {
        byte[] data = new byte[1000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        short crc = CRC16.calculate(data);
        assertTrue(CRC16.verify(data, crc));
    }

    @Test
    void calculate_withSpecialCharacters_handlesCorrectly() {
        byte[] data = "Test\n\t\r\"\\".getBytes();
        short crc = CRC16.calculate(data);
        assertTrue(CRC16.verify(data, crc));
    }
}
