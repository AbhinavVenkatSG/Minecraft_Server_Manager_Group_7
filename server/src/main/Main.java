package main;

import app.auth.AuthService;
import app.backup.BackupService;
import app.server.RestartServerService;
import app.server.ServerCatalogService;
import app.server.ServerConsoleService;
import app.server.ServerFileService;
import app.server.ServerRuntimeState;
import app.server.ServerShutdownService;
import app.server.ServerSupport;
import app.server.ServerTelemetryService;
import app.server.StartServerService;
import app.server.StopServerService;
import infra.http.ApiServer;
import infra.persistence.FileBackupStore;
import infra.persistence.FileServerStore;
import infra.persistence.FileUserStore;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = resolvePort(args);
        Path dataDirectory = Path.of("data");
        Files.createDirectories(dataDirectory);

        FileUserStore userStore = new FileUserStore(dataDirectory);
        FileServerStore serverStore = new FileServerStore(dataDirectory);
        FileBackupStore backupStore = new FileBackupStore(dataDirectory);

        AuthService authService = new AuthService(userStore);
        ServerRuntimeState serverRuntimeState = new ServerRuntimeState();
        ServerSupport serverSupport = new ServerSupport(serverStore, serverRuntimeState);
        ServerCatalogService serverCatalogService = new ServerCatalogService(serverSupport);
        StartServerService startServerService = new StartServerService(serverSupport);
        StopServerService stopServerService = new StopServerService(serverSupport);
        RestartServerService restartServerService = new RestartServerService(
                serverCatalogService,
                startServerService,
                stopServerService
        );
        ServerConsoleService serverConsoleService = new ServerConsoleService(serverSupport);
        ServerFileService serverFileService = new ServerFileService(serverSupport);
        ServerTelemetryService serverTelemetryService = new ServerTelemetryService(serverSupport);
        ServerShutdownService serverShutdownService = new ServerShutdownService(
                serverSupport,
                stopServerService,
                serverTelemetryService
        );
        BackupService backupService = new BackupService(backupStore, serverStore);

        authService.ensureInteractiveAccount();
        serverCatalogService.ensureInteractiveSetup();

        ApiServer apiServer = new ApiServer(
                port,
                authService,
                serverCatalogService,
                startServerService,
                stopServerService,
                restartServerService,
                serverConsoleService,
                serverFileService,
                serverTelemetryService,
                backupService
        );
        apiServer.start();

        System.out.println("Minecraft Server Manager API started on http://localhost:" + port);
        System.out.println("Local CLI menu is available below.");

        ConsoleMenu consoleMenu = new ConsoleMenu(
                serverCatalogService,
                startServerService,
                stopServerService,
                restartServerService,
                serverConsoleService,
                serverFileService,
                serverTelemetryService,
                backupService
        );
        try {
            consoleMenu.run();
        } finally {
            apiServer.stop(0);
            serverShutdownService.shutdown();
        }
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
