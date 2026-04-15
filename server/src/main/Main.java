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
import infra.websocket.BinaryWebSocketServer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Application entry point that wires stores, services, transports, and the local CLI.
 */
public class Main {
    /**
     * Boots the server manager and keeps it alive until the console menu exits.
     *
     * @param args optional API and websocket ports
     * @throws Exception when startup fails
     */
    public static void main(String[] args) throws Exception {
        int apiPort = resolveApiPort(args);
        int webSocketPort = resolveWebSocketPort(args, apiPort);
        Path dataDirectory = resolveDataDirectory();
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
                apiPort,
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

        BinaryWebSocketServer webSocketServer = new BinaryWebSocketServer(webSocketPort, serverSupport);
        webSocketServer.start();

        System.out.println("Minecraft Server Manager API started on http://localhost:" + apiPort);
        System.out.println("Binary WebSocket Server started on ws://localhost:" + webSocketPort + "/ws");
        System.out.println("Local CLI menu is available below.");

        ConsoleMenu consoleMenu = new ConsoleMenu(
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
        try {
            consoleMenu.run();
        } finally {
            apiServer.stop(0);
            webSocketServer.stop(0);
            serverShutdownService.shutdown();
        }
    }

    private static int resolveApiPort(String[] args) {
        if (args.length > 0) {
            return parsePort(args[0], 9000);
        }

        String envPort = System.getenv("APP_PORT");
        if (envPort != null && !envPort.isBlank()) {
            return parsePort(envPort, 9000);
        }

        return 9000;
    }

    private static int resolveWebSocketPort(String[] args, int apiPort) {
        if (args.length > 1) {
            return parsePort(args[1], apiPort + 1);
        }

        String envPort = System.getenv("WS_PORT");
        if (envPort != null && !envPort.isBlank()) {
            return parsePort(envPort, apiPort + 1);
        }

        return apiPort + 1;
    }

    private static int parsePort(String rawValue, int fallback) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Path resolveDataDirectory() throws Exception {
        Path currentWorkingData = Path.of("data").toAbsolutePath().normalize();
        if (hasPersistedData(currentWorkingData)) {
            return currentWorkingData;
        }

        Path codeLocation = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path serverDirectory = Files.isDirectory(codeLocation) ? codeLocation.getParent() : codeLocation.getParent();
        Path repositoryData = serverDirectory.getParent().resolve("data").toAbsolutePath().normalize();
        if (hasPersistedData(repositoryData) || !Files.exists(currentWorkingData)) {
            return repositoryData;
        }

        return currentWorkingData;
    }

    private static boolean hasPersistedData(Path dataDirectory) {
        return Files.exists(dataDirectory.resolve("servers").resolve("1.properties"))
                || Files.exists(dataDirectory.resolve("users").resolve("1.dat"));
    }
}
