const POLYNOMIAL = 0x1021;
const PRESET = 0xFFFF;

/**
 * Calculates the CRC16 checksum used by the binary packet protocol.
 */
function calculate(data) {
  let crc = PRESET;

  for (let i = 0; i < data.length; i++) {
    const value = data[i];
    crc ^= (value & 0xFF) << 8;

    for (let j = 0; j < 8; j++) {
      if ((crc & 0x8000) !== 0) {
        crc = (crc << 1) ^ POLYNOMIAL;
      } else {
        crc <<= 1;
      }
      crc &= 0xFFFF;
    }
  }

  return crc;
}

/**
 * Verifies a payload against an expected CRC16 value.
 */
function verify(data, expectedCrc) {
  return calculate(data) === expectedCrc;
}

export const CRC16 = {
  calculate,
  verify,
};

export default CRC16;
