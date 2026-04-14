import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from './AuthContext';
import {
  parse,
  buildCommand,
  buildHeartbeat,
  buildConsoleLog,
  buildTelemetry,
  Packet,
} from '../utils/BinaryPacket';
import { toast } from 'sonner';

const WS_URL = 'ws://localhost:8080/ws';
const API_KEY = 'minecraft_server_manager_key';

const WebSocketContext = createContext(null);

export function WebSocketProvider({ children }) {
  const { token, isAuthenticated } = useAuth();
  const [isConnected, setIsConnected] = useState(false);
  const [lastError, setLastError] = useState(null);
  const [servers, setServers] = useState([]);
  const [telemetry, setTelemetry] = useState({});
  const [consoleLogs, setConsoleLogs] = useState({});
  const [subscribedServers, setSubscribedServers] = useState(new Set());

  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const heartbeatIntervalRef = useRef(null);

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      return;
    }

    const wsUrl = `${WS_URL}?apiKey=${API_KEY}`;
    const ws = new WebSocket(wsUrl);

    ws.binaryType = 'arraybuffer';

    ws.onopen = () => {
      console.log('WebSocket connected');
      setIsConnected(true);
      setLastError(null);

      heartbeatIntervalRef.current = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(buildHeartbeat().toBytes());
        }
      }, 30000);
    };

    ws.onmessage = (event) => {
      try {
        const data = new Uint8Array(event.data);
        const packet = parse(data);
        handlePacket(packet);
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

      if (event.code !== 1000 && isAuthenticated) {
        reconnectTimeoutRef.current = setTimeout(() => {
          console.log('Attempting to reconnect...');
          connect();
        }, 3000);
      }
    };

    wsRef.current = ws;
  }, [isAuthenticated]);

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
      const bytes = packet instanceof Packet ? packet.toBytes() : packet;
      wsRef.current.send(bytes);
      return true;
    }
    console.warn('Cannot send: WebSocket not connected');
    return false;
  }, []);

  const sendCommand = useCallback((command) => {
    const packet = buildCommand(command);
    return send(packet);
  }, [send]);

  const handlePacket = useCallback((packet) => {
    const type = packet.getType();
    const payload = packet.getPayload();

    switch (type) {
      case 'RESPONSE':
        try {
          const data = JSON.parse(payload);
          if (Array.isArray(data)) {
            setServers(data);
          }
        } catch {
          toast.success(payload);
        }
        break;

      case 'CONSOLE_LOG':
        setConsoleLogs((prev) => {
          const lines = payload.split('\n').filter((l) => l);
          const serverId = extractServerId(payload);
          if (serverId) {
            return {
              ...prev,
              [serverId]: [...(prev[serverId] || []), ...lines],
            };
          }
          return prev;
        });
        break;

      case 'TELEMETRY':
        try {
          const serverId = extractServerId(payload);
          if (serverId) {
            setTelemetry((prev) => ({
              ...prev,
              [serverId]: JSON.parse(payload),
            }));
          }
        } catch {
          console.error('Invalid telemetry payload:', payload);
        }
        break;

      case 'ERROR':
        toast.error(payload);
        break;

      case 'HEARTBEAT':
        break;

      default:
        console.log('Unknown packet type:', type);
    }
  }, []);

  const extractServerId = (payload) => {
    const match = payload.match(/^(\d+):/);
    return match ? match[1] : null;
  };

  const subscribeToServer = useCallback((serverId) => {
    sendCommand(`CMD_SUBSCRIBE ${serverId}`);
    setSubscribedServers((prev) => new Set([...prev, serverId]));
  }, [sendCommand]);

  const unsubscribeFromServer = useCallback((serverId) => {
    sendCommand(`CMD_UNSUBSCRIBE ${serverId}`);
    setSubscribedServers((prev) => {
      const next = new Set(prev);
      next.delete(serverId);
      return next;
    });
  }, [sendCommand]);

  const startServer = useCallback((serverId) => {
    sendCommand(`CMD_START ${serverId}`);
  }, [sendCommand]);

  const stopServer = useCallback((serverId) => {
    sendCommand(`CMD_STOP ${serverId}`);
  }, [sendCommand]);

  const restartServer = useCallback((serverId) => {
    sendCommand(`CMD_START ${serverId}`);
    setTimeout(() => sendCommand(`CMD_STOP ${serverId}`), 2000);
  }, [sendCommand]);

  const requestServerList = useCallback(() => {
    sendCommand('CMD_LIST');
  }, [sendCommand]);

  const requestTelemetry = useCallback((serverId) => {
    sendCommand(`CMD_TELEMETRY ${serverId}`);
  }, [sendCommand]);

  const clearConsoleLogs = useCallback((serverId) => {
    setConsoleLogs((prev) => ({
      ...prev,
      [serverId]: [],
    }));
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [isAuthenticated, connect, disconnect]);

  useEffect(() => {
    if (isConnected) {
      requestServerList();
    }
  }, [isConnected, requestServerList]);

  const value = {
    isConnected,
    lastError,
    servers,
    telemetry,
    consoleLogs,
    subscribedServers,
    connect,
    disconnect,
    sendCommand,
    subscribeToServer,
    unsubscribeFromServer,
    startServer,
    stopServer,
    restartServer,
    requestServerList,
    requestTelemetry,
    clearConsoleLogs,
  };

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocket() {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within WebSocketProvider');
  }
  return context;
}

export default WebSocketContext;