package com.minecraftmanager.websocket.handler;

import com.minecraftmanager.websocket.protocol.Packet;
import com.minecraftmanager.websocket.protocol.PacketBuilder;
import com.minecraftmanager.websocket.protocol.PacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class BinaryPacketHandler extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(BinaryPacketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> consoleStreamers = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final String[] mockLogMessages = {
        "Server started on port 25565",
        "World loaded: world",
        "Player Notch joined the game",
        "Player Notch left the game",
        "Saving chunk data...",
        "Saving player data...",
        "Done saving",
        "[30m] Chunk written",
        "Player Steve joined the game",
        "Player Alex left the game",
        "Can't keep up, is the server overloaded?",
        "Block entity changed: Furnace",
        "Entity changed: Minecart",
        "Recipe shown: Diamond Sword"
    };
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        logger.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] payload = message.getPayload().array();

        try {
            Packet packet = PacketBuilder.fromBytes(payload);

            if (!packet.isValid()) {
                logger.warn("Invalid CRC for packet from session: {}", session.getId());
                sendError(session, "Invalid packet CRC");
                return;
            }

            processPacket(session, packet);

        } catch (Exception e) {
            logger.error("Error processing packet: {}", e.getMessage());
            sendError(session, "Failed to process packet: " + e.getMessage());
        }
    }

    private void processPacket(WebSocketSession session, Packet packet) {
        logger.info("Received packet type: {} from session: {}", packet.getType(), session.getId());

        switch (packet.getType()) {
            case COMMAND:
                handleCommand(session, packet.getPayload());
                break;
            case HEARTBEAT:
                handleHeartbeat(session);
                break;
            case CONSOLE_LOG:
                handleClientConsoleLog(session, packet.getPayload());
                break;
            case FILE_CHUNK:
                handleClientFileChunk(session, packet.getPayload());
                break;
            default:
                logger.warn("Unknown packet type: {}", packet.getType());
        }
    }

    private void handleCommand(WebSocketSession session, String command) {
        String[] parts = command.split(" ", 2);
        String commandType = parts[0];
        String argument = parts.length > 1 ? parts[1] : "";

        logger.info("Processing command: {} with arg: {}", commandType, argument);

        switch (commandType) {
            case "CMD_START":
                handleStartServer(session, argument);
                break;
            case "CMD_STOP":
                handleStopServer(session, argument);
                break;
            case "CMD_BACKUP":
                handleBackupRequest(session, argument);
                break;
            case "CMD_CONSOLE":
                sendResponse(session, "Console command sent: " + argument);
                break;
            case "CMD_STATUS":
                boolean isRunning = consoleStreamers.containsKey(argument);
                sendResponse(session, "Server status: " + (isRunning ? "Running" : "Stopped"));
                break;
            default:
                sendError(session, "Unknown command: " + commandType);
        }
    }

    private void handleStartServer(WebSocketSession session, String serverName) {
        if (consoleStreamers.containsKey(serverName)) {
            sendResponse(session, "Server already running: " + serverName);
            return;
        }

        sendResponse(session, "Server start requested: " + serverName);
        startConsoleStreaming(session, serverName);

        sendConsoleLog(session, "[INFO] Server started on port 25565");
        sendConsoleLog(session, "[INFO] World loaded: world");
    }

    private void handleStopServer(WebSocketSession session, String serverName) {
        stopConsoleStreaming(serverName);
        sendResponse(session, "Server stop requested: " + serverName);

        sendConsoleLog(session, "[INFO] Stopping server...");
        sendConsoleLog(session, "[INFO] Server stopped");
    }

    private void handleBackupRequest(WebSocketSession session, String serverName) {
        sendResponse(session, "BACKUP_STARTING...");

        new Thread(() -> {
            try {
                Thread.sleep(500);
                sendFileChunks(session, serverName);
                Thread.sleep(500);
                sendResponse(session, "BACKUP_COMPLETE");
            } catch (InterruptedException e) {
                logger.error("Backup interrupted: {}", e.getMessage());
            }
        }).start();
    }

    private void startConsoleStreaming(WebSocketSession session, String serverName) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        consoleStreamers.put(serverName, scheduler);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                String logMessage = mockLogMessages[random.nextInt(mockLogMessages.length)];
                String formattedLog = "[" + timeFormatter.format(LocalDateTime.now()) + "] " + logMessage;
                sendConsoleLog(session, formattedLog);
            } catch (Exception e) {
                logger.error("Error sending console log: {}", e.getMessage());
            }
        }, 3, 3, TimeUnit.SECONDS);

        logger.info("Started console streaming for server: {}", serverName);
    }

    private void stopConsoleStreaming(String serverName) {
        ScheduledExecutorService scheduler = consoleStreamers.remove(serverName);
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            logger.info("Stopped console streaming for server: {}", serverName);
        }
    }

    private void sendConsoleLog(WebSocketSession session, String message) {
        try {
            Packet consoleLog = PacketBuilder.buildConsoleLog(message);
            session.sendMessage(new BinaryMessage(PacketBuilder.toBytes(consoleLog)));
        } catch (IOException e) {
            logger.error("Failed to send console log: {}", e.getMessage());
        }
    }

    private void sendFileChunks(WebSocketSession session, String serverName) {
        try {
            byte[] mockData = generateMockBackupData(serverName);
            int chunkSize = 64 * 1024;
            int totalChunks = (int) Math.ceil((double) mockData.length / chunkSize);

            logger.info("Sending {} chunks for backup", totalChunks);

            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, mockData.length);
                byte[] chunkData = new byte[end - start];
                System.arraycopy(mockData, start, chunkData, 0, chunkData.length);

                Packet chunk = PacketBuilder.buildFileChunk(chunkData, i + 1, totalChunks);
                session.sendMessage(new BinaryMessage(PacketBuilder.toBytes(chunk)));

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to send file chunks: {}", e.getMessage());
            sendError(session, "Backup failed: " + e.getMessage());
        }
    }

    private byte[] generateMockBackupData(String serverName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder sb = new StringBuilder();

        baos.write(("Backup for server: " + serverName + "\n").getBytes());
        baos.write(("Timestamp: " + LocalDateTime.now() + "\n\n").getBytes());

        for (int i = 0; i < 3000; i++) {
            sb.append("Mock data line ").append(i).append(": ");
            for (int j = 0; j < 50; j++) {
                sb.append((char) ('a' + random.nextInt(26)));
            }
            sb.append("\n");
        }

        baos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    private void handleClientConsoleLog(WebSocketSession session, String payload) {
        logger.info("Received console log from client: {}", payload);
        sendResponse(session, "Console log received: " + payload);
    }

    private void handleClientFileChunk(WebSocketSession session, String payload) {
        logger.info("Received file chunk from client: {}", payload.substring(0, Math.min(50, payload.length())));
        sendResponse(session, "File chunk received");
    }

    private void handleHeartbeat(WebSocketSession session) {
        try {
            Packet heartbeat = PacketBuilder.buildHeartbeat();
            session.sendMessage(new BinaryMessage(PacketBuilder.toBytes(heartbeat)));
        } catch (IOException e) {
            logger.error("Failed to send heartbeat: {}", e.getMessage());
        }
    }

    private void sendResponse(WebSocketSession session, String message) {
        try {
            Packet response = PacketBuilder.buildResponse(message);
            session.sendMessage(new BinaryMessage(PacketBuilder.toBytes(response)));
        } catch (IOException e) {
            logger.error("Failed to send response: {}", e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Packet error = PacketBuilder.buildError(errorMessage);
            session.sendMessage(new BinaryMessage(PacketBuilder.toBytes(error)));
        } catch (IOException e) {
            logger.error("Failed to send error: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        
        consoleStreamers.entrySet().removeIf(entry -> {
            entry.getValue().shutdown();
            return true;
        });
        
        logger.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Transport error for session: {}", session.getId(), exception);
        sessions.remove(session.getId());
    }
}