const PacketType = {
  COMMAND: 0x01,
  RESPONSE: 0x02,
  CONSOLE_LOG: 0x03,
  HEARTBEAT: 0x04,
  FILE_CHUNK: 0x05,
  ERROR: 0x06,
  TELEMETRY: 0x07,
};

function fromValue(value) {
  for (const key in PacketType) {
    if (PacketType[key] === value) {
      return key;
    }
  }
  throw new Error(`Unknown packet type: ${value}`);
}

function toValue(name) {
  if (PacketType[name] !== undefined) {
    return PacketType[name];
  }
  throw new Error(`Unknown packet type name: ${name}`);
}

export { PacketType, fromValue, toValue };
export default PacketType;