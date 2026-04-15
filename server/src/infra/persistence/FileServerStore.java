package infra.persistence;

import app.server.ServerStore;
import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * File-backed {@link ServerStore} implementation for managed server metadata.
 */
public class FileServerStore implements ServerStore {
    private final Path serverDirectory;
    private final AtomicLong nextId = new AtomicLong(1L);
    private final ConcurrentHashMap<Long, ManagedServer> servers = new ConcurrentHashMap<>();

    /**
     * Creates the store, ensures its directories exist, and loads persisted servers.
     *
     * @param rootDirectory application data directory
     */
    public FileServerStore(Path rootDirectory) {
        try {
            this.serverDirectory = rootDirectory.resolve("servers");
            Files.createDirectories(serverDirectory);
            loadServers();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize server store.", exception);
        }
    }

    @Override
    /**
     * Returns every persisted server ordered by id.
     */
    public List<ManagedServer> findAll() {
        List<ManagedServer> values = new ArrayList<>(servers.values());
        values.sort(Comparator.comparingLong(ManagedServer::getId));
        return values;
    }

    @Override
    /**
     * Finds a server by id.
     *
     * @param id server id
     * @return matching server when found
     */
    public Optional<ManagedServer> findById(long id) {
        return Optional.ofNullable(servers.get(id));
    }

    @Override
    /**
     * Saves a server, assigning the next numeric id when needed.
     *
     * @param server server to persist
     * @return the saved server
     */
    public synchronized ManagedServer save(ManagedServer server) {
        if (server.getId() == 0L) {
            server.setId(nextId.getAndIncrement());
        } else {
            nextId.set(Math.max(nextId.get(), server.getId() + 1));
        }

        servers.put(server.getId(), server);
        writeServer(server);
        return server;
    }

    private void loadServers() throws Exception {
        try (var paths = Files.list(serverDirectory)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .forEach(this::readServerUnchecked);
        }
    }

    private void readServerUnchecked(Path path) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(Files.readString(path)));

            ManagedServer server = new ManagedServer(
                    Long.parseLong(properties.getProperty("id", "0")),
                    properties.getProperty("name", ""),
                    properties.getProperty("host", "localhost"),
                    Integer.parseInt(properties.getProperty("port", "25565")),
                    properties.getProperty("rconPassword", ""),
                    Integer.parseInt(properties.getProperty("rconPort", "25575")),
                    ServerStatus.valueOf(properties.getProperty("status", ServerStatus.STARTUP.name())),
                    properties.getProperty("serverProperties", ""),
                    properties.getProperty("backupPath", ""),
                    properties.getProperty("minecraftDirectory", ""),
                    Long.parseLong(properties.getProperty("ownerId", "0")),
                    LocalDateTime.parse(properties.getProperty("createdAt")),
                    properties.getProperty("lastStarted", "").isBlank()
                            ? null
                            : LocalDateTime.parse(properties.getProperty("lastStarted"))
            );

            servers.put(server.getId(), server);
            nextId.set(Math.max(nextId.get(), server.getId() + 1));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load server file: " + path, exception);
        }
    }

    private void writeServer(ManagedServer server) {
        try {
            Properties properties = new Properties();
            properties.setProperty("id", Long.toString(server.getId()));
            properties.setProperty("name", server.getName());
            properties.setProperty("host", server.getHost());
            properties.setProperty("port", Integer.toString(server.getPort()));
            properties.setProperty("rconPassword", server.getRconPassword());
            properties.setProperty("rconPort", Integer.toString(server.getRconPort()));
            properties.setProperty("status", server.getStatus().name());
            properties.setProperty("serverProperties", server.getServerProperties());
            properties.setProperty("backupPath", server.getBackupPath());
            properties.setProperty("minecraftDirectory", server.getMinecraftDirectory());
            properties.setProperty("ownerId", Long.toString(server.getOwnerId()));
            properties.setProperty("createdAt", server.getCreatedAt().toString());
            properties.setProperty("lastStarted", server.getLastStarted() == null ? "" : server.getLastStarted().toString());

            StringWriter writer = new StringWriter();
            properties.store(writer, null);
            Files.writeString(serverDirectory.resolve(server.getId() + ".properties"), writer.toString());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist server.", exception);
        }
    }
}
