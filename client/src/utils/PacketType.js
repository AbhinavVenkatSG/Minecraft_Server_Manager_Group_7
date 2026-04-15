const PacketType = {
  COMMAND: 0x01,
  RESPONSE: 0x02,
  CONSOLE_LOG: 0x03,
  HEARTBEAT: 0x04,
  FILE_CHUNK: 0x05,
  ERROR: 0x06,
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
