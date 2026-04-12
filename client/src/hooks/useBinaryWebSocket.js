import { useState, useEffect, useRef, useCallback } from 'react';
import { BinaryPacket, PacketType } from '../utils/BinaryPacket';

const useBinaryWebSocket = (url, apiKey, onMessage) => {
  const [isConnected, setIsConnected] = useState(false);
  const [lastError, setLastError] = useState(null);
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const heartbeatIntervalRef = useRef(null);

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      return;
    }

    const wsUrl = `${url}?apiKey=${apiKey}`;
    const ws = new WebSocket(wsUrl);

    ws.binaryType = 'arraybuffer';

    ws.onopen = () => {
      console.log('WebSocket connected');
      setIsConnected(true);
      setLastError(null);

      heartbeatIntervalRef.current = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          const heartbeat = BinaryPacket.buildHeartbeat();
          ws.send(heartbeat.toBytes());
        }
      }, 30000);
    };

    ws.onmessage = (event) => {
      try {
        const data = new Uint8Array(event.data);
        const packet = BinaryPacket.parse(data);

        if (onMessage) {
          onMessage(packet);
        }
      } catch (error) {
        console.error('Error parsing packet:', error);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      setLastError('WebSocket connection error');
    };

    ws.onclose = (event) => {
      console.log('WebSocket closed:', event.code, event.reason);
      setIsConnected(false);

      if (heartbeatIntervalRef.current) {
        clearInterval(heartbeatIntervalRef.current);
      }

      if (event.code !== 1000) {
        reconnectTimeoutRef.current = setTimeout(() => {
          console.log('Attempting to reconnect...');
          connect();
        }, 3000);
      }
    };

    wsRef.current = ws;
  }, [url, apiKey, onMessage]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }

    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }

    if (wsRef.current) {
      wsRef.current.close(1000, 'Client disconnecting');
      wsRef.current = null;
    }

    setIsConnected(false);
  }, []);

  const send = useCallback((packet) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      const bytes = packet instanceof BinaryPacket ? packet.toBytes() : packet;
      wsRef.current.send(bytes);
      return true;
    }
    console.warn('Cannot send: WebSocket not connected');
    return false;
  }, []);

  const sendCommand = useCallback((command) => {
    const packet = BinaryPacket.buildCommand(command);
    return send(packet);
  }, [send]);

  const sendConsoleCommand = useCallback((command) => {
    const packet = BinaryPacket.buildCommand(`CMD_CONSOLE ${command}`);
    return send(packet);
  }, [send]);

  useEffect(() => {
    connect();

    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    isConnected,
    lastError,
    send,
    sendCommand,
    sendConsoleCommand,
    disconnect,
    reconnect: connect
  };
};

export default useBinaryWebSocket;