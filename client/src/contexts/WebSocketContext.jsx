import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from './AuthContext';
import { parse, buildHeartbeat } from '../utils/BinaryPacket';
import { toast } from 'sonner';
import api from '../services/api';

const WS_URL = (import.meta.env.VITE_WS_URL || 'ws://localhost:9001/ws').replace(/\/$/, '');
const API_KEY = 'minecraft_server_manager_key';

const WebSocketContext = createContext(null);

/**
 * Coordinates websocket liveness and the REST-backed server data used by the dashboard.
 */
export function WebSocketProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [isConnected, setIsConnected] = useState(false);
  const [lastError, setLastError] = useState(null);
  const [servers, setServers] = useState([]);
  const [telemetry, setTelemetry] = useState({});
  const [consoleLogs, setConsoleLogs] = useState({});
  const [subscribedServers, setSubscribedServers] = useState(new Set());

  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const heartbeatIntervalRef = useRef(null);

  const markConnected = useCallback(() => {
    setIsConnected(true);
    setLastError(null);
  }, []);

  const handlePacket = useCallback((packet) => {
    switch (packet.getType()) {
      case 'RESPONSE':
        toast.success(packet.getPayload());
        break;
      case 'ERROR':
        toast.error(packet.getPayload());
        break;
      case 'HEARTBEAT':
        break;
      default:
        break;
    }
  }, []);

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN || !isAuthenticated) {
      return;
    }

    const ws = new WebSocket(`${WS_URL}?apiKey=${API_KEY}`);
    ws.binaryType = 'arraybuffer';

    ws.onopen = () => {
      // The server expects a lightweight keepalive so idle connections stay warm.
      heartbeatIntervalRef.current = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(buildHeartbeat().toBytes());
        }
      }, 30000);
    };

    ws.onmessage = (event) => {
      try {
        handlePacket(parse(new Uint8Array(event.data)));
      } catch (error) {
        console.error('Error parsing packet:', error);
      }
    };

    ws.onerror = () => {
      setLastError('WebSocket connection error');
    };

    ws.onclose = (event) => {
      if (heartbeatIntervalRef.current) {
        clearInterval(heartbeatIntervalRef.current);
      }

      wsRef.current = null;

      if (event.code !== 1000 && isAuthenticated) {
        // A short retry loop is enough here because the dashboard is local-first.
        reconnectTimeoutRef.current = setTimeout(() => {
          connect();
        }, 3000);
      }
    };

    wsRef.current = ws;
  }, [handlePacket, isAuthenticated]);

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

  const requestServerList = useCallback(async () => {
    try {
      const response = await api.getServers();
      if (response.status === 200 && Array.isArray(response.data)) {
        setServers(response.data);
        markConnected();
        return response.data;
      }

      throw new Error(response.data?.message || 'Failed to load servers');
    } catch (error) {
      setLastError(error.message);
      setIsConnected(false);
      return [];
    }
  }, [markConnected]);

  const requestTelemetry = useCallback(async (serverId) => {
    if (!serverId) {
      return null;
    }

    try {
      const response = await api.getTelemetry(serverId);
      if (response.status !== 200 || !response.data) {
        throw new Error(response.data?.message || 'Failed to load telemetry');
      }

      const nextTelemetry = mapTelemetry(response.data);
      setTelemetry((prev) => ({
        ...prev,
        [serverId]: nextTelemetry,
      }));

      if (nextTelemetry.newLogLines.length > 0) {
        setConsoleLogs((prev) => ({
          ...prev,
          [serverId]: mergeConsoleLines(prev[serverId] || [], nextTelemetry.newLogLines),
        }));
      }

      markConnected();
      return nextTelemetry;
    } catch (error) {
      setLastError(error.message);
      return null;
    }
  }, [markConnected]);

  const requestConsoleLogs = useCallback(async (serverId) => {
    if (!serverId) {
      return [];
    }

    try {
      const response = await api.getConsoleLog(serverId);
      if (response.status !== 200) {
        throw new Error(response.data?.message || 'Failed to load console output');
      }

      const lines = Array.isArray(response.data?.lines) ? response.data.lines : [];
      setConsoleLogs((prev) => ({
        ...prev,
        [serverId]: lines,
      }));
      markConnected();
      return lines;
    } catch (error) {
      setLastError(error.message);
      return [];
    }
  }, [markConnected]);

  const sendConsoleCommand = useCallback(async (serverId, command) => {
    if (!serverId || !command?.trim()) {
      return { success: false, error: 'Missing server or command' };
    }

    try {
      const response = await api.sendConsoleCommand(serverId, command.trim());
      if (response.status !== 200) {
        return { success: false, error: response.data?.message || 'Failed to send command' };
      }

      await requestConsoleLogs(serverId);
      markConnected();
      return { success: true, message: response.data?.message || 'Command sent' };
    } catch (error) {
      setLastError(error.message);
      return { success: false, error: error.message };
    }
  }, [markConnected, requestConsoleLogs]);

  const subscribeToServer = useCallback((serverId) => {
    setSubscribedServers((prev) => new Set([...prev, serverId]));
  }, []);

  const unsubscribeFromServer = useCallback((serverId) => {
    setSubscribedServers((prev) => {
      const next = new Set(prev);
      next.delete(serverId);
      return next;
    });
  }, []);

  const startServer = useCallback(async (serverId) => {
    const response = await api.startServer(serverId);
    await requestServerList();
    return response;
  }, [requestServerList]);

  const stopServer = useCallback(async (serverId) => {
    const response = await api.stopServer(serverId);
    await requestServerList();
    return response;
  }, [requestServerList]);

  const restartServer = useCallback(async (serverId) => {
    const response = await api.restartServer(serverId);
    await requestServerList();
    return response;
  }, [requestServerList]);

  const sendCommand = useCallback(async (command) => {
    const raw = command?.trim();
    if (!raw) {
      return false;
    }

    if (raw === 'CMD_LIST') {
      await requestServerList();
      return true;
    }

    if (raw.startsWith('CMD_TELEMETRY ')) {
      const serverId = Number(raw.split(/\s+/)[1]);
      await requestTelemetry(serverId);
      return true;
    }

    if (raw.startsWith('CMD_START ')) {
      const serverId = Number(raw.split(/\s+/)[1]);
      await startServer(serverId);
      return true;
    }

    if (raw.startsWith('CMD_STOP ')) {
      const serverId = Number(raw.split(/\s+/)[1]);
      await stopServer(serverId);
      return true;
    }

    if (raw.startsWith('CMD_CONSOLE ')) {
      const parts = raw.split(' ');
      const serverId = Number(parts[1]);
      const consoleCommand = parts.slice(2).join(' ');
      const result = await sendConsoleCommand(serverId, consoleCommand);
      return result.success;
    }

    return false;
  }, [requestServerList, requestTelemetry, restartServer, sendConsoleCommand, startServer, stopServer]);

  const clearConsoleLogs = useCallback((serverId) => {
    setConsoleLogs((prev) => ({
      ...prev,
      [serverId]: [],
    }));
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      disconnect();
      setServers([]);
      setTelemetry({});
      setConsoleLogs({});
      setSubscribedServers(new Set());
      return;
    }

    connect();
    requestServerList();

    return () => {
      disconnect();
    };
  }, [connect, disconnect, isAuthenticated, requestServerList]);

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
    sendConsoleCommand,
    subscribeToServer,
    unsubscribeFromServer,
    startServer,
    stopServer,
    restartServer,
    requestServerList,
    requestTelemetry,
    requestConsoleLogs,
    clearConsoleLogs,
  };

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  );
}

/**
 * Returns websocket-driven dashboard state and actions.
 */
export function useWebSocket() {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within WebSocketProvider');
  }
  return context;
}

export default WebSocketContext;

/**
 * Adds a few UI-friendly derived fields on top of the raw telemetry payload.
 */
function mapTelemetry(snapshot) {
  const isRunning = snapshot.operationalState === 'RUNNING';
  const memoryLimitBytes = parseJvmRamToBytes(snapshot.jvmAllocatedRam);
  const cpuUsage = isRunning ? round(snapshot.minecraftProcessCpuLoadPercent ?? 0) : 0;
  const memoryBytes = isRunning ? Math.max(0, snapshot.minecraftProcessMemoryBytes ?? 0) : 0;
  const memoryUsage = memoryLimitBytes > 0
    ? round((memoryBytes / memoryLimitBytes) * 100)
    : 0;
  const storageBytes = Math.max(0, snapshot.minecraftDirectorySizeBytes ?? 0);

  return {
    ...snapshot,
    isRunning,
    cpuUsage,
    memoryBytes,
    memoryLimitBytes,
    memoryUsage,
    storageBytes,
    cpuInfo: isRunning ? 'Minecraft server process' : 'Server stopped',
    memoryInfo: memoryLimitBytes > 0
      ? `${formatBytes(memoryBytes)} / ${formatBytes(memoryLimitBytes)}`
      : formatBytes(memoryBytes),
    storageInfo: 'Minecraft server directory size',
    newLogLines: Array.isArray(snapshot.newLogLines) ? snapshot.newLogLines : [],
  };
}

/**
 * Keeps only the most recent console lines to avoid unbounded client-side growth.
 */
function mergeConsoleLines(existing, incoming) {
  const combined = [...existing, ...incoming];
  return combined.slice(-250);
}

function round(value) {
  return Math.round((value || 0) * 10) / 10;
}

function formatBytes(value) {
  if (!value) {
    return '0 B';
  }

  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let size = value;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }

  return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

/**
 * Converts JVM RAM flags like {@code 2G} or {@code 512M} into bytes.
 */
function parseJvmRamToBytes(value) {
  if (!value) {
    return 0;
  }

  const match = String(value).trim().match(/^(\d+(?:\.\d+)?)([kmg])?b?$/i);
  if (!match) {
    return 0;
  }

  const amount = Number(match[1]);
  const unit = (match[2] || 'b').toUpperCase();

  switch (unit) {
    case 'K':
      return Math.round(amount * 1024);
    case 'M':
      return Math.round(amount * 1024 * 1024);
    case 'G':
      return Math.round(amount * 1024 * 1024 * 1024);
    default:
      return Math.round(amount);
  }
}
