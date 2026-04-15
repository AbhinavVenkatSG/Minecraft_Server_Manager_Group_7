/**
 * @file BinaryPacket.js
 * @brief Binary packet serialization and parsing for WebSocket communication.
 * @{
 */

import CRC16 from './CRC16';
import { PacketType, toValue } from './PacketType';

/**
 * @class Packet
 * @brief Represents a binary packet for WebSocket communication.
 * @details Packets contain type, CRC checksum, length, and UTF-8 payload.
 */
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

/**
 * Parses one binary frame into a Packet instance.
 */
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
 * Builds a command packet.
 */
function buildCommand(command) {
  return new Packet('COMMAND', command);
}

/**
 * Builds a generic response packet.
 */
function buildResponse(message) {
  return new Packet('RESPONSE', message);
}

/**
 * Builds a console log packet.
 */
function buildConsoleLog(message) {
  return new Packet('CONSOLE_LOG', message);
}

/**
 * Builds a heartbeat packet.
 */
function buildHeartbeat() {
  return new Packet('HEARTBEAT', 'ping');
}

/**
 * Builds a file chunk packet.
 */
function buildFileChunk(payload) {
  return new Packet('FILE_CHUNK', payload);
}

/**
 * Builds an error packet.
 */
function buildError(message) {
  return new Packet('ERROR', message);
}

/**
 * Builds a telemetry packet.
 */
function buildTelemetry(payload) {
  return new Packet('TELEMETRY', payload);
}

export { Packet, parse, buildCommand, buildResponse, buildConsoleLog, buildHeartbeat, buildFileChunk, buildError, buildTelemetry };
export default { Packet, parse, buildCommand, buildResponse, buildConsoleLog, buildHeartbeat, buildFileChunk, buildError, buildTelemetry };
