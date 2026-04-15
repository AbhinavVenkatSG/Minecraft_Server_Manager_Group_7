/**
 * @file CRC16.js
 * @brief CRC16-CCITT checksum implementation for packet validation.
 * @{
 */

/**
 * @brief CRC polynomial for CCITT algorithm
 */
const POLYNOMIAL = 0x1021;
/**
 * @brief Initial CRC value
 */
const PRESET = 0xFFFF;

/**
 * @brief Calculates the CRC16 checksum used by the binary packet protocol.
 * @param {Uint8Array|number[]} data - Input byte array
 * @returns {number} CRC16 checksum value
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
 * @brief Verifies a payload against an expected CRC16 value.
 * @param {Uint8Array|number[]} data - Input byte array
 * @param {number} expectedCrc - Expected CRC value
 * @returns {boolean} True if CRC matches
 */
function verify(data, expectedCrc) {
  return calculate(data) === expectedCrc;
}

export const CRC16 = {
  calculate,
  verify,
};

export default CRC16;
