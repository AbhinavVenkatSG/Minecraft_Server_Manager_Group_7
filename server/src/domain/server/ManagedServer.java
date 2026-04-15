/**
 * @file ManagedServer.java
 * @brief Domain entity representing a managed Minecraft server instance.
 * @{
 */

package domain.server;

import java.time.LocalDateTime;

/**
 * @class ManagedServer
 * @brief Represents a managed Minecraft server configuration.
 * @details Contains server connection details, status, paths, and metadata.
 */
public class ManagedServer {
    private long id;
    private final String name;
    private final String host;
    private final int port;
    private final String rconPassword;
    private final int rconPort;
    private ServerStatus status;
    private String serverProperties;
    private String backupPath;
    private String minecraftDirectory;
    private final long ownerId;
    private final LocalDateTime createdAt;
    private LocalDateTime lastStarted;

    public ManagedServer(
            long id,
            String name,
            String host,
            int port,
            String rconPassword,
            int rconPort,
            ServerStatus status,
            String serverProperties,
            String backupPath,
            String minecraftDirectory,
            long ownerId,
            LocalDateTime createdAt,
            LocalDateTime lastStarted
    ) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.rconPassword = rconPassword;
        this.rconPort = rconPort;
        this.status = status;
        this.serverProperties = serverProperties;
        this.backupPath = backupPath;
        this.minecraftDirectory = minecraftDirectory;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.lastStarted = lastStarted;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getRconPassword() {
        return rconPassword;
    }

    public int getRconPort() {
        return rconPort;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    public String getServerProperties() {
        return serverProperties;
    }

    public void setServerProperties(String serverProperties) {
        this.serverProperties = serverProperties;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public String getMinecraftDirectory() {
        return minecraftDirectory;
    }

    public void setMinecraftDirectory(String minecraftDirectory) {
        this.minecraftDirectory = minecraftDirectory;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastStarted() {
        return lastStarted;
    }

    public void setLastStarted(LocalDateTime lastStarted) {
        this.lastStarted = lastStarted;
    }
}
