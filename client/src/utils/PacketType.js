/**
 * @file PacketType.js
 * @brief Packet type constants matching the server protocol.
 * @{
 */

/**
 * @namespace PacketType
 * @brief Binary packet type identifiers used in WebSocket communication.
 */
const PacketType = {
  /** Command packet type (0x01) */
  COMMAND: 0x01,
  /** Response packet type (0x02) */
  RESPONSE: 0x02,
  /** Console log packet type (0x03) */
  CONSOLE_LOG: 0x03,
  /** Heartbeat packet type (0x04) */
  HEARTBEAT: 0x04,
  /** File chunk packet type (0x05) */
  FILE_CHUNK: 0x05,
  /** Error packet type (0x06) */
  ERROR: 0x06,
  /** Telemetry packet type (0x07) */
  TELEMETRY: 0x07,
};

/**
 * Resolves the packet type name from the numeric wire value.
 */
function fromValue(value) {
  for (const key in PacketType) {
    if (PacketType[key] === value) {
      return key;
    }
  }
  throw new Error(`Unknown packet type: ${value}`);
}

/**
 * Resolves the numeric wire value for a packet type name.
 */
function toValue(name) {
  if (PacketType[name] !== undefined) {
    return PacketType[name];
  }
  throw new Error(`Unknown packet type name: ${name}`);
}

export { PacketType, fromValue, toValue };
export default PacketType;
