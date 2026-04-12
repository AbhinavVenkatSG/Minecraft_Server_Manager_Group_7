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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BinaryPacketHandler extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(BinaryPacketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

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
            case FILE_CHUNK:
                logger.info("Received {} - handling not implemented yet", packet.getType());
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
                sendResponse(session, "Server start requested: " + argument);
                break;
            case "CMD_STOP":
                sendResponse(session, "Server stop requested: " + argument);
                break;
            case "CMD_BACKUP":
                sendResponse(session, "Backup requested: " + argument);
                break;
            case "CMD_CONSOLE":
                sendResponse(session, "Console command sent: " + argument);
                break;
            case "CMD_STATUS":
                sendResponse(session, "Server status: Running");
                break;
            default:
                sendError(session, "Unknown command: " + commandType);
        }
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
        logger.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Transport error for session: {}", session.getId(), exception);
        sessions.remove(session.getId());
    }
}