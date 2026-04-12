import CRC16 from './crc16';

const PacketType = {
  COMMAND: 0x01,
  RESPONSE: 0x02,
  CONSOLE_LOG: 0x03,
  HEARTBEAT: 0x04,
  FILE_CHUNK: 0x05,
  ERROR: 0x06
};

class BinaryPacket {
  constructor(type, payload) {
    this.type = type;
    this.payload = payload;
    this.crc = 0;
    this.length = 0;
  }

  static build(type, payload) {
    return new BinaryPacket(type, payload);
  }

  static buildCommand(command) {
    return new BinaryPacket(PacketType.COMMAND, command);
  }

  static buildResponse(message) {
    return new BinaryPacket(PacketType.RESPONSE, message);
  }

  static buildConsoleLog(log) {
    return new BinaryPacket(PacketType.CONSOLE_LOG, log);
  }

  static buildHeartbeat() {
    return new BinaryPacket(PacketType.HEARTBEAT, 'ping');
  }

  static buildFileChunk(chunkData, chunkNumber, totalChunks) {
    const payload = `${chunkNumber}|${totalChunks}|${chunkData}`;
    return new BinaryPacket(PacketType.FILE_CHUNK, payload);
  }

  static buildError(errorMessage) {
    return new BinaryPacket(PacketType.ERROR, errorMessage);
  }

  toBytes() {
    const encoder = new TextEncoder();
    const payloadBytes = encoder.encode(this.payload);
    this.crc = CRC16.calculate(payloadBytes);
    this.length = payloadBytes.length;

    const buffer = new ArrayBuffer(5 + payloadBytes.length);
    const view = new DataView(buffer);

    view.setUint8(0, this.type);
    view.setUint16(1, this.crc, false);
    view.setUint16(3, this.length, false);

    const payloadArray = new Uint8Array(buffer, 5);
    payloadArray.set(payloadBytes);

    return new Uint8Array(buffer);
  }

  static parse(data) {
    if (data instanceof ArrayBuffer) {
      data = new Uint8Array(data);
    }

    if (data.length < 5) {
      throw new Error('Invalid packet: too short');
    }

    const view = new DataView(data.buffer, data.byteOffset, data.length);
    const type = view.getUint8(0);
    const crc = view.getUint16(1, false);
    const length = view.getUint16(3, false);

    const payloadBytes = data.slice(5, 5 + length);
    const decoder = new TextDecoder();
    const payload = decoder.decode(payloadBytes);

    const packet = new BinaryPacket(type, payload);
    packet.crc = crc;
    packet.length = length;

    return packet;
  }

  isValid() {
    const encoder = new TextEncoder();
    const payloadBytes = encoder.encode(this.payload);
    return CRC16.verify(payloadBytes, this.crc);
  }

  getTypeName() {
    switch (this.type) {
      case PacketType.COMMAND: return 'COMMAND';
      case PacketType.RESPONSE: return 'RESPONSE';
      case PacketType.CONSOLE_LOG: return 'CONSOLE_LOG';
      case PacketType.HEARTBEAT: return 'HEARTBEAT';
      case PacketType.FILE_CHUNK: return 'FILE_CHUNK';
      case PacketType.ERROR: return 'ERROR';
      default: return 'UNKNOWN';
    }
  }
}

export { BinaryPacket, PacketType };
export default BinaryPacket;