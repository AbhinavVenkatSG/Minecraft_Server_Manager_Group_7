package infra.websocket;

import app.server.*;
import core.protocol.Packet;
import core.protocol.PacketBuilder;
import core.protocol.PacketType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * Binary websocket endpoint used by the client for command, status, and telemetry packets.
 */
public class BinaryWebSocketServer extends WebSocketServer {
    private static final String EXPECTED_API_KEY = "minecraft_server_manager_key";
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

    private final Map<WebSocket, ClientSession> clients = new ConcurrentHashMap<>();
    private final ServerSupport serverSupport;
    private final CommandHandler commandHandler;
    private final ScheduledExecutorService heartbeatScheduler;

    /**
     * Creates the websocket server and packet command handler.
     *
     * @param port websocket port to bind
     * @param serverSupport shared server helpers and runtime state
     * @throws IOException when the socket cannot be created
     */
    public BinaryWebSocketServer(int port, ServerSupport serverSupport) throws IOException {
        super(new InetSocketAddress(port));
        this.serverSupport = serverSupport;
        this.commandHandler = new CommandHandler(serverSupport, this);
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        super.start();
        startHeartbeatScheduler();
    }

    @Override
    public void stop(int timeout) {
        heartbeatScheduler.shutdown();
        try {
            heartbeatScheduler.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            super.stop(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void startHeartbeatScheduler() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            for (Map.Entry<WebSocket, ClientSession> entry : clients.entrySet()) {
                WebSocket conn = entry.getKey();
                if (conn.isOpen()) {
                    sendPacket(conn, PacketBuilder.buildHeartbeat());
                }
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String apiKey = handshake.getFieldValue("apiKey");
        if (apiKey == null) {
            String queryString = handshake.getResourceDescriptor();
            if (queryString != null && queryString.contains("apiKey=")) {
                int start = queryString.indexOf("apiKey=") + 7;
                int end = queryString.indexOf("&", start);
                if (end == -1) end = queryString.length();
                apiKey = queryString.substring(start, end);
            }
        }

        if (!EXPECTED_API_KEY.equals(apiKey)) {
            sendPacket(conn, PacketBuilder.buildError("Invalid or missing API key."));
            conn.close(1008, "Invalid API key");
            return;
        }

        clients.put(conn, new ClientSession());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        sendPacket(conn, PacketBuilder.buildError("Text messages not supported. Use binary frames."));
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        ClientSession session = clients.get(conn);
        if (session == null) {
            sendPacket(conn, PacketBuilder.buildError("Session not found."));
            return;
        }

        try {
            byte[] data = new byte[message.remaining()];
            message.get(data);
            Packet packet = Packet.fromBytes(data);

            if (!packet.isValid()) {
                sendPacket(conn, PacketBuilder.buildError("CRC validation failed."));
                return;
            }

            PacketRouter.route(conn, packet, commandHandler, session);
        } catch (IllegalArgumentException e) {
            sendPacket(conn, PacketBuilder.buildError("Invalid packet format: " + e.getMessage()));
        } catch (Exception e) {
            sendPacket(conn, PacketBuilder.buildError("Processing error: " + e.getMessage()));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            System.err.println("WebSocket error for " + conn.getRemoteSocketAddress() + ": " + ex.getMessage());
        } else {
            System.err.println("WebSocket server error: " + ex.getMessage());
        }
    }

    @Override
    public void onStart() {
        System.out.println("Binary WebSocket Server started on " + getAddress());
    }

    /**
     * Sends one packet to a connected client.
     *
     * @param conn websocket connection
     * @param packet packet to serialize and send
     */
    public void sendPacket(WebSocket conn, Packet packet) {
        if (conn != null && conn.isOpen()) {
            try {
                conn.send(packet.toBytes());
            } catch (Exception e) {
                System.err.println("Failed to send packet: " + e.getMessage());
            }
        }
    }

    /**
     * Broadcasts a packet to every connected websocket client.
     *
     * @param packet packet to send
     */
    public void broadcastPacket(Packet packet) {
        for (WebSocket conn : clients.keySet()) {
            sendPacket(conn, packet);
        }
    }

    /**
     * Sends a packet only to clients subscribed to a specific server id.
     *
     * @param serverId managed server id
     * @param packet packet to send
     */
    public void sendToAllServerSubscribers(long serverId, Packet packet) {
        for (Map.Entry<WebSocket, ClientSession> entry : clients.entrySet()) {
            if (entry.getValue().subscribedServers.contains(serverId)) {
                sendPacket(entry.getKey(), packet);
            }
        }
    }

    /**
     * Returns the number of currently connected websocket clients.
     *
     * @return live client count
     */
    public int getConnectedClientCount() {
        return clients.size();
    }

    /**
     * Per-connection state kept alongside the websocket session.
     */
    public static class ClientSession {
        public final java.util.Set<Long> subscribedServers = ConcurrentHashMap.newKeySet();
        public String authToken;
    }
}
