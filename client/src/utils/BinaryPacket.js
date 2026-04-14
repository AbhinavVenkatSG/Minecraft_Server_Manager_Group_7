import CRC16 from './CRC16';
import { PacketType, toValue } from './PacketType';

class Packet {
  constructor(type, payload) {
    this.type = type;
    this.payload = payload || '';
  }

  toBytes() {
    const payloadBytes = new TextEncoder().encode(this.payload);
    const length = payloadBytes.length;
    const crc = CRC16.calculate(payloadBytes);

    const buffer = new ArrayBuffer(5 + length);
    const view = new DataView(buffer);

    view.setUint8(0, toValue(this.type));
    view.setUint16(1, crc, false);
    view.setUint16(3, length, false);

    const payloadArray = new Uint8Array(buffer, 5);
    payloadArray.set(payloadBytes);

    return new Uint8Array(buffer);
  }

  getType() {
    return this.type;
  }

  getPayload() {
    return this.payload;
  }

  getCrc() {
    const payloadBytes = new TextEncoder().encode(this.payload);
    return CRC16.calculate(payloadBytes);
  }

  getLength() {
    return this.payload.length;
  }

  isValid() {
    const payloadBytes = new TextEncoder().encode(this.payload);
    return CRC16.verify(payloadBytes, this.getCrc());
  }
}

function parse(data) {
  if (data.length < 5) {
    throw new Error('Packet is too short.');
  }

  const view = new DataView(data.buffer, data.byteOffset);
  const typeValue = view.getUint8(0);
  const crc = view.getUint16(1, false);
  const length = view.getUint16(3, false);

  if (length > data.length - 5) {
    throw new Error('Invalid packet length.');
  }

  const payloadBytes = data.slice(5, 5 + length);
  const payload = new TextDecoder().decode(payloadBytes);

  const type = fromValue(typeValue);

  return new Packet(type, payload);
}

function fromValue(value) {
  for (const key in PacketType) {
    if (PacketType[key] === value) {
      return key;
    }
  }
  throw new Error(`Unknown packet type: ${value}`);
}

function buildCommand(command) {
  return new Packet('COMMAND', command);
}

function buildResponse(message) {
  return new Packet('RESPONSE', message);
}

function buildConsoleLog(message) {
  return new Packet('CONSOLE_LOG', message);
}

function buildHeartbeat() {
  return new Packet('HEARTBEAT', 'ping');
}

function buildFileChunk(payload) {
  return new Packet('FILE_CHUNK', payload);
}

function buildError(message) {
  return new Packet('ERROR', message);
}

function buildTelemetry(payload) {
  return new Packet('TELEMETRY', payload);
}

export { Packet, parse, buildCommand, buildResponse, buildConsoleLog, buildHeartbeat, buildFileChunk, buildError, buildTelemetry };
export default { Packet, parse, buildCommand, buildResponse, buildConsoleLog, buildHeartbeat, buildFileChunk, buildError, buildTelemetry };