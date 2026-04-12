import { createContext, useContext, useState, useCallback } from 'react';
import useBinaryWebSocket from '../hooks/useBinaryWebSocket';
import { BinaryPacket, PacketType } from '../utils/BinaryPacket';

const WebSocketContext = createContext(null);

const WS_URL = 'ws://localhost:8080/ws';
const API_KEY = 'minecraft_server_manager_key';

export const WebSocketProvider = ({ children }) => {
  const [consoleLogs, setConsoleLogs] = useState([]);
  const [lastResponse, setLastResponse] = useState(null);
  const [lastError, setLastError] = useState(null);
  const [transferProgress, setTransferProgress] = useState(null);

  const handleMessage = useCallback((packet) => {
    switch (packet.type) {
      case PacketType.RESPONSE:
        setLastResponse(packet.payload);
        break;

      case PacketType.CONSOLE_LOG:
        setConsoleLogs(prev => [...prev.slice(-99), packet.payload]);
        break;

      case PacketType.HEARTBEAT:
        console.log('Heartbeat received');
        break;

      case PacketType.FILE_CHUNK:
        const parts = packet.payload.split('|');
        if (parts.length >= 3) {
          const chunkNumber = parseInt(parts[0], 10);
          const totalChunks = parseInt(parts[1], 10);
          setTransferProgress({
            current: chunkNumber,
            total: totalChunks,
            percentage: Math.round((chunkNumber / totalChunks) * 100)
          });
        }
        break;

      case PacketType.ERROR:
        setLastError(packet.payload);
        break;

      default:
        console.log('Unknown packet type:', packet.type);
    }
  }, []);

  const {
    isConnected,
    lastError: wsError,
    sendCommand,
    sendConsoleCommand,
    disconnect,
    reconnect
  } = useBinaryWebSocket(WS_URL, API_KEY, handleMessage);

  const sendStartCommand = useCallback((serverName) => {
    return sendCommand(`CMD_START ${serverName}`);
  }, [sendCommand]);

  const sendStopCommand = useCallback((serverName) => {
    return sendCommand(`CMD_STOP ${serverName}`);
  }, [sendCommand]);

  const sendBackupCommand = useCallback((serverName) => {
    return sendCommand(`CMD_BACKUP ${serverName}`);
  }, [sendCommand]);

  const sendStatusCommand = useCallback((serverName) => {
    return sendCommand(`CMD_STATUS ${serverName}`);
  }, [sendCommand]);

  const sendCustomCommand = useCallback((command) => {
    return sendCommand(command);
  }, [sendCommand]);

  const clearConsoleLogs = useCallback(() => {
    setConsoleLogs([]);
  }, []);

  const clearError = useCallback(() => {
    setLastError(null);
  }, []);

  const clearTransferProgress = useCallback(() => {
    setTransferProgress(null);
  }, []);

  const value = {
    isConnected,
    consoleLogs,
    lastResponse,
    lastError: lastError || wsError,
    transferProgress,
    sendStartCommand,
    sendStopCommand,
    sendBackupCommand,
    sendStatusCommand,
    sendConsoleCommand,
    sendCustomCommand,
    clearConsoleLogs,
    clearError,
    clearTransferProgress,
    disconnect,
    reconnect
  };

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocketContext = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocketContext must be used within a WebSocketProvider');
  }
  return context;
};

export default WebSocketContext;