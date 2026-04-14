package core.util;

public final class CRC16 {
    private static final int POLYNOMIAL = 0x1021;
    private static final int PRESET = 0xFFFF;

    private CRC16() {
    }

    public static short calculate(byte[] data) {
        int crc = PRESET;

        for (byte value : data) {
            crc ^= (value & 0xFF) << 8;

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ POLYNOMIAL;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }

        return (short) crc;
    }

    public static boolean verify(byte[] data, short expectedCrc) {
        return calculate(data) == expectedCrc;
    }
}
