package infra.websocket;

import app.server.CommandHandler;
import core.protocol.Packet;
import core.protocol.PacketBuilder;
import core.protocol.PacketType;
import org.java_websocket.WebSocket;

public class PacketRouter {

    public static void route(WebSocket conn, Packet packet, CommandHandler commandHandler, BinaryWebSocketServer.ClientSession session) {
        switch (packet.getType()) {
            case COMMAND:
                handleCommand(conn, packet, commandHandler, session);
                break;
            case HEARTBEAT:
                handleHeartbeat(conn);
                break;
            case RESPONSE:
            case CONSOLE_LOG:
            case FILE_CHUNK:
            case ERROR:
            case TELEMETRY:
            default:
                sendError(conn, "Unexpected packet type: " + packet.getType());
        }
    }

    private static void handleCommand(WebSocket conn, Packet packet, CommandHandler commandHandler, BinaryWebSocketServer.ClientSession session) {
        String payload = packet.getPayload();
        if (payload == null || payload.isEmpty()) {
            sendError(conn, "Command payload is empty.");
            return;
        }

        String[] parts = payload.split(" ", 3);
        if (parts.length == 0) {
            sendError(conn, "Invalid command format.");
            return;
        }

        String command = parts[0].toUpperCase();

        try {
            switch (command) {
                case "CMD_START":
                    if (parts.length < 2) {
                        sendError(conn, "Usage: CMD_START <serverId>");
                        return;
                    }
                    long startServerId = parseServerId(parts[1]);
                    commandHandler.handleStartServer(conn, startServerId);
                    break;

                case "CMD_STOP":
                    if (parts.length < 2) {
                        sendError(conn, "Usage: CMD_STOP <serverId>");
                        return;
                    }
                    long stopServerId = parseServerId(parts[1]);
                    commandHandler.handleStopServer(conn, stopServerId);
                    break;

                case "CMD_STATUS":
                    if (parts.length < 2) {
                        sendError(conn, "Usage: CMD_STATUS <serverId>");
                        return;
                    }
                    long statusServerId = parseServerId(parts[1]);
                    commandHandler.handleStatus(conn, statusServerId);
                    break;

                case "CMD_BACKUP":
                    if (parts.length < 2) {
                        sendError(conn, "Usage: CMD_BACKUP <serverId>");
                        return;
                    }
                    long backupServerId = parseServerId(parts[1]);
                    commandHandler.handleBackup(conn, backupServerId);
                    break;

                case "CMD_CONSOLE":
                    if (parts.length < 3) {
                        sendError(conn, "Usage: CMD_CONSOLE <serverId> <command>");
                        return;
                    }
                    long consoleServerId = parseServerId(parts[1]);
                    String consoleCommand = parts[2];
                    commandHandler.handleConsole(conn, consoleServerId, consoleCommand);
                    break;

                case "CMD_TELEMETRY":
                    if (parts.length < 2) {
                        sendError(conn, "Usage: CMD_TELEMETRY <serverId>");
                        return;
                    }
                    long telemetryServerId = parseServerId(parts[1]);
                    commandHandler.handleTelemetry(conn, telemetryServerId);
                    break;

                case "CMD_SUBSCRIBE":
                    if (parts.length < 2) {
                        sendError(conn, "Usage: CMD_SUBSCRIBE <serverId>");
                        return;
                    }
                    long subscribeServerId = parseServerId(parts[1]);
                    commandHandler.handleSubscribe(conn, session, subscribeServerId);
                    break;

                case "CMD_UNSUBSCRIBE":
                    if (parts.length < 2) {
                        sendError(conn, "Usage: CMD_UNSUBSCRIBE <serverId>");
                        return;
                    }
                    long unsubscribeServerId = parseServerId(parts[1]);
                    commandHandler.handleUnsubscribe(conn, session, unsubscribeServerId);
                    break;

                case "CMD_LIST":
                    commandHandler.handleListServers(conn);
                    break;

                case "CMD_LIST_PLAYERS":
                    if (parts.length < 2) {
                        sendError(conn, "Usage: CMD_LIST_PLAYERS <serverId>");
                        return;
                    }
                    long playersServerId = parseServerId(parts[1]);
                    commandHandler.handleListPlayers(conn, playersServerId);
                    break;

                default:
                    sendError(conn, "Unknown command: " + command + ". Supported: CMD_START, CMD_STOP, CMD_STATUS, CMD_BACKUP, CMD_CONSOLE, CMD_TELEMETRY, CMD_SUBSCRIBE, CMD_UNSUBSCRIBE, CMD_LIST, CMD_LIST_PLAYERS");
            }
        } catch (NumberFormatException e) {
            sendError(conn, "Invalid server ID format: " + parts[1]);
        } catch (Exception e) {
            sendError(conn, "Command execution failed: " + e.getMessage());
        }
    }

    private static void handleHeartbeat(WebSocket conn) {
        sendPacket(conn, PacketBuilder.buildHeartbeat());
    }

    private static void sendError(WebSocket conn, String message) {
        sendPacket(conn, PacketBuilder.buildError(message));
    }

    private static void sendPacket(WebSocket conn, Packet packet) {
        if (conn != null && conn.isOpen()) {
            try {
                conn.send(packet.toBytes());
            } catch (Exception e) {
                System.err.println("Failed to send packet: " + e.getMessage());
            }
        }
    }

    private static long parseServerId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid server ID: " + value);
        }
    }
}
