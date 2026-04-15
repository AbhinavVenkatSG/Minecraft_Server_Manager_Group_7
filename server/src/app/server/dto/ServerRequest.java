/**
 * @file ServerRequest.java
 * @brief Data transfer object for creating or updating a managed server.
 * @{
 */

package app.server.dto;

/**
 * @class ServerRequest
 * @brief DTO containing server configuration for creation or updates.
 */
public class ServerRequest {
    private final String name;
    private final String host;
    private final int port;
    private final String rconPassword;
    private final int rconPort;
    private final String serverProperties;
    private final String backupPath;
    private final String minecraftDirectory;

    /**
     * @brief Constructs a server request.
     * @param name Server display name
     * @param host Server hostname
     * @param port Server port
     * @param rconPassword RCON password
     * @param rconPort RCON port
     * @param serverProperties Server properties content
     * @param backupPath Backup directory path
     * @param minecraftDirectory Minecraft installation directory
     */
    public ServerRequest(
            String name,
            String host,
            int port,
            String rconPassword,
            int rconPort,
            String serverProperties,
            String backupPath,
            String minecraftDirectory
    ) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.rconPassword = rconPassword;
        this.rconPort = rconPort;
        this.serverProperties = serverProperties;
        this.backupPath = backupPath;
        this.minecraftDirectory = minecraftDirectory;
    }

    /**
     * @return The server name
     */
    public String getName() {
        return name;
    }

    /**
     * @return The server host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return The server port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return The RCON password
     */
    public String getRconPassword() {
        return rconPassword;
    }

    /**
     * @return The RCON port
     */
    public int getRconPort() {
        return rconPort;
    }

    /**
     * @return The server.properties content
     */
    public String getServerProperties() {
        return serverProperties;
    }

    /**
     * @return The backup path
     */
    public String getBackupPath() {
        return backupPath;
    }

    /**
     * @return The Minecraft directory path
     */
    public String getMinecraftDirectory() {
        return minecraftDirectory;
    }
}
