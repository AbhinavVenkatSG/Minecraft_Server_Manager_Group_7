package app.server;

import app.server.dto.ServerRequest;
import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ServerService {
    private final ServerStore serverStore;

    public ServerService(ServerStore serverStore) {
        this.serverStore = serverStore;
    }

    public List<ManagedServer> listServers() {
        return serverStore.findAll();
    }

    public Optional<ManagedServer> getServer(long serverId) {
        return serverStore.findById(serverId);
    }

    public Optional<ManagedServer> createServer(ServerRequest request, long ownerId) {
        if (request == null || isBlank(request.getName()) || isBlank(request.getHost()) || request.getPort() <= 0) {
            return Optional.empty();
        }

        ManagedServer server = new ManagedServer(
                0L,
                request.getName().trim(),
                request.getHost().trim(),
                request.getPort(),
                safe(request.getRconPassword()),
                request.getRconPort() > 0 ? request.getRconPort() : 25575,
                ServerStatus.STOPPED,
                safe(request.getServerProperties()),
                safe(request.getBackupPath()),
                ownerId,
                LocalDateTime.now(),
                null
        );

        return Optional.of(serverStore.save(server));
    }

    public Optional<ManagedServer> startServer(long serverId) {
        Optional<ManagedServer> server = serverStore.findById(serverId);
        server.ifPresent(found -> {
            found.setStatus(ServerStatus.RUNNING);
            found.setLastStarted(LocalDateTime.now());
            serverStore.save(found);
        });
        return server;
    }

    public Optional<ManagedServer> stopServer(long serverId) {
        Optional<ManagedServer> server = serverStore.findById(serverId);
        server.ifPresent(found -> {
            found.setStatus(ServerStatus.STOPPED);
            serverStore.save(found);
        });
        return server;
    }

    public void seedSampleData() {
        if (!serverStore.findAll().isEmpty()) {
            return;
        }

        createServer(new ServerRequest(
                "My First Server",
                "localhost",
                25565,
                "changeme",
                25575,
                "motd=A beginner server",
                "backups/world-one"
        ), 1L);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
