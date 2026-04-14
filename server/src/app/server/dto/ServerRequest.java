package app.server.dto;

public class ServerRequest {
    private final String name;
    private final String host;
    private final int port;
    private final String rconPassword;
    private final int rconPort;
    private final String serverProperties;
    private final String backupPath;

    public ServerRequest(
            String name,
            String host,
            int port,
            String rconPassword,
            int rconPort,
            String serverProperties,
            String backupPath
    ) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.rconPassword = rconPassword;
        this.rconPort = rconPort;
        this.serverProperties = serverProperties;
        this.backupPath = backupPath;
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

    public String getServerProperties() {
        return serverProperties;
    }

    public String getBackupPath() {
        return backupPath;
    }
}
