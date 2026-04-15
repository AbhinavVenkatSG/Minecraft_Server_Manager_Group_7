/**
 * @file CommandHandler.java
 * @brief Handles incoming WebSocket commands and routes them to appropriate services.
 * @ingroup app
 * @{
 */

package app.server;

import app.backup.BackupService;
import core.protocol.Packet;
import core.protocol.PacketBuilder;
import domain.server.ManagedServer;
import domain.server.ServerStatus;
import domain.server.TelemetrySnapshot;
import infra.websocket.BinaryWebSocketServer;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * @class CommandHandler
 * @brief Processes incoming WebSocket commands and sends responses.
 * @details Routes commands for server control, telemetry, console, and subscriptions.
 */
public class CommandHandler {
    private final ServerSupport serverSupport;
    private final BinaryWebSocketServer webSocketServer;

    public CommandHandler(ServerSupport serverSupport, BinaryWebSocketServer webSocketServer) {
        this.serverSupport = serverSupport;
        this.webSocketServer = webSocketServer;
    }

    public void handleStartServer(WebSocket conn, long serverId) {
        try {
            Optional<ManagedServer> server = serverSupport.getServer(serverId);
            if (server.isEmpty()) {
                sendError(conn, "Server not found with ID: " + serverId);
                return;
            }

            ManagedServer managedServer = server.get();
            if (managedServer.getStatus() == ServerStatus.RUNNING) {
                sendResponse(conn, "Server is already running: " + managedServer.getName());
                return;
            }

            java.nio.file.Path mcDir;
            try {
                mcDir = serverSupport.requireMinecraftDirectory(managedServer);
            } catch (Exception e) {
                sendError(conn, "Cannot start server: " + e.getMessage());
                return;
            }

            try {
                managedServer.setStatus(ServerStatus.BLOCKED);
                serverSupport.saveServer(managedServer);

                java.lang.Process process = new ProcessBuilder("cmd.exe", "/c", "start.bat")
                        .directory(mcDir.toFile())
                        .redirectErrorStream(true)
                        .start();

                ServerRuntimeState.RuntimeContext context = new ServerRuntimeState.RuntimeContext(process);
                serverSupport.getRuntimeState().getRuntimeContexts().put(serverId, context);
                serverSupport.startOutputPump(serverId, managedServer, context);

                managedServer.setStatus(ServerStatus.RUNNING);
                managedServer.setLastStarted(java.time.LocalDateTime.now());
                serverSupport.saveServer(managedServer);

                sendResponse(conn, "Server started: " + managedServer.getName() + " (ID: " + serverId + ")");
            } catch (Exception e) {
                serverSupport.getRuntimeState().getRuntimeContexts().remove(serverId);
                managedServer.setStatus(ServerStatus.STOPPED);
                serverSupport.saveServer(managedServer);
                sendError(conn, "Failed to start server: " + e.getMessage());
            }
        } catch (Exception e) {
            sendError(conn, "Start server error: " + e.getMessage());
        }
    }

    public void handleStopServer(WebSocket conn, long serverId) {
        try {
            Optional<ManagedServer> server = serverSupport.getServer(serverId);
            if (server.isEmpty()) {
                sendError(conn, "Server not found with ID: " + serverId);
                return;
            }

            ManagedServer managedServer = server.get();
            if (managedServer.getStatus() != ServerStatus.RUNNING) {
                sendResponse(conn, "Server is not running: " + managedServer.getName());
                return;
            }

            ServerRuntimeState.RuntimeContext context = serverSupport.getRuntimeState().getRuntimeContexts().get(serverId);
            if (context == null) {
                sendError(conn, "Server runtime context not found.");
                return;
            }

            managedServer.setStatus(ServerStatus.BLOCKED);
            serverSupport.saveServer(managedServer);

            try {
                context.getInput().write("stop");
                context.getInput().newLine();
                context.getInput().flush();
                
                sendResponse(conn, "Stop command sent to server: " + managedServer.getName());
            } catch (Exception e) {
                sendError(conn, "Failed to send stop command: " + e.getMessage());
            }
        } catch (Exception e) {
            sendError(conn, "Stop server error: " + e.getMessage());
        }
    }

    public void handleStatus(WebSocket conn, long serverId) {
        Optional<ManagedServer> server = serverSupport.getServer(serverId);
        if (server.isEmpty()) {
            sendError(conn, "Server not found with ID: " + serverId);
            return;
        }

        ManagedServer managedServer = server.get();
        String status = String.format(
            "Server: %s | Status: %s | Host: %s:%d | MC Directory: %s",
            managedServer.getName(),
            managedServer.getStatus(),
            managedServer.getHost(),
            managedServer.getPort(),
            managedServer.getMinecraftDirectory() != null ? managedServer.getMinecraftDirectory() : "Not configured"
        );
        sendResponse(conn, status);
    }

    public void handleBackup(WebSocket conn, long serverId) {
        Optional<ManagedServer> server = serverSupport.getServer(serverId);
        if (server.isEmpty()) {
            sendError(conn, "Server not found with ID: " + serverId);
            return;
        }

        String filename = "backup-" + serverId + "-" + System.currentTimeMillis() + ".zip";
        String filePath = "backups/" + filename;
        
        sendResponse(conn, "Backup initiated for server " + serverId + ": " + filename);
        sendConsoleLog(conn, "[BTPServer] Backup feature pending implementation");
    }

    public void handleConsole(WebSocket conn, long serverId, String command) {
        Optional<ManagedServer> server = serverSupport.getServer(serverId);
        if (server.isEmpty()) {
            sendError(conn, "Server not found with ID: " + serverId);
            return;
        }

        ManagedServer managedServer = server.get();
        ServerRuntimeState.RuntimeContext context = serverSupport.getRuntimeState().getRuntimeContexts().get(serverId);
        
        if (managedServer.getStatus() != ServerStatus.RUNNING || context == null) {
            sendError(conn, "Server is not running.");
            return;
        }

        try {
            context.getInput().write(command.trim());
            context.getInput().newLine();
            context.getInput().flush();
            sendResponse(conn, "Command sent: " + command);
        } catch (Exception e) {
            sendError(conn, "Failed to send command: " + e.getMessage());
        }
    }

    public void handleTelemetry(WebSocket conn, long serverId) {
        Optional<ManagedServer> server = serverSupport.getServer(serverId);
        if (server.isEmpty()) {
            sendError(conn, "Server not found with ID: " + serverId);
            return;
        }

        ManagedServer managedServer = server.get();
        ServerRuntimeState.RuntimeContext context = serverSupport.getRuntimeState().getRuntimeContexts().get(serverId);

        if (managedServer.getStatus() != ServerStatus.RUNNING || context == null) {
            sendError(conn, "Server is not running.");
            return;
        }

        try {
            OptionalDouble cpuUsage = serverSupport.readProcessCpuLoad(context);
            long memoryBytes = serverSupport.readProcessMemoryBytes(context);
            List<String> players = serverSupport.readOnlinePlayers(serverSupport.requireMinecraftDirectory(managedServer).resolve("logs/latest.log"));

            String telemetry = String.format(
                "CPU: %s | Memory: %.1fMB | Players: %d | Status: %s",
                cpuUsage.isPresent() ? String.format("%.1f%%", cpuUsage.getAsDouble()) : "Failed to read",
                memoryBytes / (1024.0 * 1024.0),
                players.size(),
                managedServer.getStatus()
            );
            sendPacket(conn, PacketBuilder.buildTelemetry(telemetry));
        } catch (Exception e) {
            sendError(conn, "Failed to get telemetry: " + e.getMessage());
        }
    }

    public void handleSubscribe(WebSocket conn, BinaryWebSocketServer.ClientSession session, long serverId) {
        Optional<ManagedServer> server = serverSupport.getServer(serverId);
        if (server.isEmpty()) {
            sendError(conn, "Server not found with ID: " + serverId);
            return;
        }

        session.subscribedServers.add(serverId);
        sendResponse(conn, "Subscribed to server " + serverId);
    }

    public void handleUnsubscribe(WebSocket conn, BinaryWebSocketServer.ClientSession session, long serverId) {
        session.subscribedServers.remove(serverId);
        sendResponse(conn, "Unsubscribed from server " + serverId);
    }

    public void handleListPlayers(WebSocket conn, long serverId) {
        Optional<ManagedServer> server = serverSupport.getServer(serverId);
        if (server.isEmpty()) {
            sendError(conn, "Server not found with ID: " + serverId);
            return;
        }

        ManagedServer managedServer = server.get();
        
        if (managedServer.getStatus() != ServerStatus.RUNNING) {
            sendResponse(conn, "Server is not running. No players online.");
            return;
        }

        try {
            java.nio.file.Path logPath = serverSupport.requireMinecraftDirectory(managedServer).resolve("logs/latest.log");
            List<String> players = serverSupport.readOnlinePlayers(logPath);
            
            if (players.isEmpty()) {
                sendResponse(conn, "No players online.");
            } else {
                sendResponse(conn, "Online players (" + players.size() + "): " + String.join(", ", players));
            }
        } catch (Exception e) {
            sendError(conn, "Failed to get players: " + e.getMessage());
        }
    }

    public void broadcastConsoleLog(long serverId, String logLine) {
        Packet packet = PacketBuilder.buildConsoleLog(logLine);
        webSocketServer.sendToAllServerSubscribers(serverId, packet);
    }

    private void sendResponse(WebSocket conn, String message) {
        sendPacket(conn, PacketBuilder.buildResponse(message));
    }

    private void sendError(WebSocket conn, String message) {
        sendPacket(conn, PacketBuilder.buildError(message));
    }

    private void sendConsoleLog(WebSocket conn, String message) {
        sendPacket(conn, PacketBuilder.buildConsoleLog(message));
    }

    private void sendPacket(WebSocket conn, Packet packet) {
        if (conn != null && conn.isOpen()) {
            try {
                conn.send(packet.toBytes());
            } catch (Exception e) {
                System.err.println("Failed to send packet: " + e.getMessage());
            }
        }
    }
}
