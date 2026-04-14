package main;

import app.auth.AuthService;
import app.backup.BackupService;
import app.server.ServerService;
import infra.http.ApiServer;
import infra.persistence.InMemoryBackupStore;
import infra.persistence.InMemoryServerStore;
import infra.persistence.InMemoryUserStore;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = resolvePort(args);
        InMemoryUserStore userStore = new InMemoryUserStore();
        InMemoryServerStore serverStore = new InMemoryServerStore();
        InMemoryBackupStore backupStore = new InMemoryBackupStore();

        AuthService authService = new AuthService(userStore);
        ServerService serverService = new ServerService(serverStore);
        BackupService backupService = new BackupService(backupStore, serverStore);

        serverService.seedSampleData();

        ApiServer apiServer = new ApiServer(port, authService, serverService, backupService);
        apiServer.start();

        System.out.println("Minecraft Server Manager API started on http://localhost:" + port);
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0) {
            return parsePort(args[0], 8080);
        }

        String envPort = System.getenv("APP_PORT");
        if (envPort != null && !envPort.isBlank()) {
            return parsePort(envPort, 8080);
        }

        return 8080;
    }

    private static int parsePort(String rawValue, int fallback) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
