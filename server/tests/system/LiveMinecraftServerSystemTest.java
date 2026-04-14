package system;

import app.auth.AuthService;
import app.backup.BackupService;
import app.server.RestartServerService;
import app.server.ServerCatalogService;
import app.server.ServerConsoleService;
import app.server.ServerFileService;
import app.server.ServerRuntimeState;
import app.server.ServerSupport;
import app.server.ServerTelemetryService;
import app.server.StartServerService;
import app.server.StopServerService;
import domain.backup.Backup;
import domain.server.ManagedServer;
import domain.server.ServerStatus;
import domain.server.TelemetrySnapshot;
import infra.http.ApiServer;
import infra.http.Json;
import infra.persistence.InMemoryBackupStore;
import infra.persistence.InMemoryServerStore;
import infra.persistence.InMemoryUserStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Properties;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("system")
class LiveMinecraftServerSystemTest {

    private Path minecraftDirectory;
    private Path startScript;
    private Path serverPropertiesFile;
    private Path whitelistFile;
    private Path backupDirectory;
    private byte[] originalStartScript;
    private byte[] originalServerProperties;
    private byte[] originalWhitelist;
    private InMemoryServerStore serverStore;
    private ServerRuntimeState runtimeState;
    private ServerCatalogService serverCatalogService;
    private StartServerService startServerService;
    private StopServerService stopServerService;
    private ServerConsoleService serverConsoleService;
    private ServerFileService serverFileService;
    private ServerTelemetryService serverTelemetryService;
    private BackupService backupService;
    private ApiServer apiServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        Properties configuredServer = loadConfiguredServer(Path.of("data", "servers", "1.properties"));
        minecraftDirectory = Path.of(configuredServer.getProperty("minecraftDirectory"));
        Assumptions.assumeTrue(Files.isDirectory(minecraftDirectory), "Configured Minecraft directory is missing.");

        String host = configuredServer.getProperty("host", "localhost");
        int port = Integer.parseInt(configuredServer.getProperty("port", "25565"));
        Assumptions.assumeFalse(canConnect(host, port), "Configured Minecraft server must be stopped before running system tests.");
        serverPropertiesFile = minecraftDirectory.resolve("server.properties");
        Assumptions.assumeTrue(
                Files.isReadable(serverPropertiesFile) && Files.isWritable(serverPropertiesFile),
                "Live system tests require readable and writable access to the configured Minecraft server files."
        );

        serverStore = new InMemoryServerStore();
        runtimeState = new ServerRuntimeState();
        ServerSupport serverSupport = new ServerSupport(serverStore, runtimeState);
        serverCatalogService = new ServerCatalogService(serverSupport);
        startServerService = new StartServerService(serverSupport);
        stopServerService = new StopServerService(serverSupport);
        RestartServerService restartServerService = new RestartServerService(
                serverCatalogService,
                startServerService,
                stopServerService
        );
        serverConsoleService = new ServerConsoleService(serverSupport);
        serverFileService = new ServerFileService(serverSupport);
        serverTelemetryService = new ServerTelemetryService(serverSupport);
        backupDirectory = Files.createTempDirectory("mc-live-backups");
        backupService = new BackupService(new InMemoryBackupStore(), serverStore);

        ManagedServer server = new ManagedServer(
                1L,
                configuredServer.getProperty("name", "Live Server"),
                host,
                port,
                configuredServer.getProperty("rconPassword", ""),
                Integer.parseInt(configuredServer.getProperty("rconPort", "25575")),
                ServerStatus.STOPPED,
                Files.readString(serverPropertiesFile),
                backupDirectory.toString(),
                minecraftDirectory.toString(),
                Long.parseLong(configuredServer.getProperty("ownerId", "1")),
                LocalDateTime.now(),
                null
        );
        serverStore.save(server);

        startScript = serverSupport.resolveStartScript(minecraftDirectory);
        whitelistFile = serverSupport.resolveWhitelistFile(minecraftDirectory);
        Assumptions.assumeTrue(
                Files.isWritable(startScript)
                        && Files.isWritable(serverPropertiesFile)
                        && Files.isWritable(minecraftDirectory)
                        && (!Files.exists(whitelistFile) || Files.isWritable(whitelistFile)),
                "Live system tests require write access to the configured Minecraft directory."
        );
        originalStartScript = Files.readAllBytes(startScript);
        originalServerProperties = Files.readAllBytes(serverPropertiesFile);
        originalWhitelist = Files.exists(whitelistFile) ? Files.readAllBytes(whitelistFile) : null;

        Files.writeString(startScript, stripPause(Files.readString(startScript)));

        int portForApi = findFreePort();
        apiServer = new ApiServer(
                portForApi,
                new AuthService(new InMemoryUserStore()),
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

        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + portForApi;
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (!runtimeState.getRuntimeContexts().isEmpty()) {
                stopServerService.stopServer(1L);
            }
        } catch (Exception ignored) {
        }

        if (apiServer != null) {
            apiServer.stop(0);
        }
        if (serverTelemetryService != null) {
            serverTelemetryService.shutdown();
        }

        if (originalStartScript != null) {
            restoreFile(startScript, originalStartScript);
        }
        if (originalServerProperties != null) {
            restoreFile(serverPropertiesFile, originalServerProperties);
        }
        if (whitelistFile != null) {
            restoreFile(whitelistFile, originalWhitelist);
        }

        if (backupDirectory != null && Files.exists(backupDirectory)) {
            try (var paths = Files.walk(backupDirectory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    @Test
    void services_canManageLiveConfiguredServer() throws Exception {
        String originalWhitelistText = Files.exists(whitelistFile) ? Files.readString(whitelistFile) : "[]";
        String originalStartParameters = serverFileService.readStartParameters(1L);

        serverFileService.writeServerProperties(1L, Files.readString(serverPropertiesFile) + System.lineSeparator() + "# live service test");
        serverFileService.writeWhitelist(1L, originalWhitelistText + System.lineSeparator());
        serverFileService.writeStartParameters(1L, originalStartParameters + " -Dcodex.live.service=true");

        assertTrue(serverFileService.readServerProperties(1L).contains("# live service test"));
        assertEquals(originalWhitelistText + System.lineSeparator(), serverFileService.readWhitelist(1L));
        assertTrue(serverFileService.readStartParameters(1L).contains("-Dcodex.live.service=true"));

        ManagedServer started = startServerService.startServer(1L).orElseThrow();
        waitUntil(
                () -> serverConsoleService.getRecentConsoleLines(1L).stream().anyMatch(line -> line.contains("Done")),
                Duration.ofSeconds(60),
                "Live Minecraft server never became ready."
        );

        serverConsoleService.sendConsoleCommand(1L, "list");
        waitUntil(
                () -> serverConsoleService.getRecentConsoleLines(1L).stream().anyMatch(line -> line.contains("There are 0 of a max of 20 players online:")),
                Duration.ofSeconds(15),
                "Live Minecraft server never responded to the list command."
        );

        TelemetrySnapshot snapshot = serverTelemetryService.getTelemetry(1L);

        assertEquals(ServerStatus.RUNNING, started.getStatus());
        assertEquals(ServerStatus.RUNNING, snapshot.getOperationalState());
        assertTrue(snapshot.getMinecraftDirectorySizeBytes() > 0L);
        assertFalse(snapshot.getPacketBase64().isBlank());
        assertFalse(snapshot.getJvmAllocatedRam().isBlank());

        ManagedServer stopped = stopServerService.stopServer(1L).orElseThrow();
        Backup backup = backupService.createBackup(1L).orElseThrow();

        assertEquals(ServerStatus.STOPPED, stopped.getStatus());
        assertTrue(Files.exists(Path.of(backup.getFilePath())));
        assertTrue(Path.of(backup.getFilePath()).startsWith(backupDirectory));
    }

    @Test
    void api_canManageLiveConfiguredServer_andServerRoutesRemainUnauthenticated() throws Exception {
        HttpResponse<String> register = sendRequest(
                "POST",
                "/api/auth/register",
                "{\"username\":\"live-system-user\",\"password\":\"password123\"}"
        );
        HttpResponse<String> login = sendRequest(
                "POST",
                "/api/auth/login",
                "{\"username\":\"live-system-user\",\"password\":\"password123\"}"
        );
        HttpResponse<String> readDirectory = sendRequest("GET", "/api/servers/1/directory", null);
        HttpResponse<String> readProperties = sendRequest("GET", "/api/servers/1/files/server-properties", null);
        HttpResponse<String> readWhitelist = sendRequest("GET", "/api/servers/1/files/whitelist", null);
        HttpResponse<String> startParameters = sendRequest("GET", "/api/servers/1/start-parameters", null);
        HttpResponse<String> updateStartParameters = sendRequest(
                "POST",
                "/api/servers/1/start-parameters",
                "{\"content\":" + Json.quote(Json.readString(startParameters.body(), "content") + " -Dcodex.live.api=true") + "}"
        );
        HttpResponse<String> start = sendRequest("POST", "/api/servers/1/start", "{}");

        assertEquals(200, register.statusCode());
        assertEquals(200, login.statusCode());
        assertFalse(Json.readString(login.body(), "token").isBlank());
        assertEquals(200, readDirectory.statusCode());
        assertEquals(minecraftDirectory.toString(), Json.readString(readDirectory.body(), "minecraftDirectory"));
        assertEquals(200, readProperties.statusCode());
        assertTrue(Json.readString(readProperties.body(), "content").contains("server-port=25565"));
        assertEquals(200, readWhitelist.statusCode());
        assertFalse(Json.readString(readWhitelist.body(), "content").isBlank());
        assertEquals(200, updateStartParameters.statusCode());
        assertEquals(200, start.statusCode());

        waitUntil(
                () -> getConsoleLogBody().contains("Done"),
                Duration.ofSeconds(60),
                "Live Minecraft server never became ready through the API."
        );

        HttpResponse<String> sendConsole = sendRequest("POST", "/api/servers/1/console", "{\"command\":\"list\"}");
        waitUntil(
                () -> getConsoleLogBody().contains("There are 0 of a max of 20 players online:"),
                Duration.ofSeconds(15),
                "Live Minecraft server never responded to the API console command."
        );

        HttpResponse<String> telemetry = sendRequest("GET", "/api/servers/1/telemetry", null);
        HttpResponse<String> stop = sendRequest("POST", "/api/servers/1/stop", "{}");
        HttpResponse<String> createBackup = sendRequest("POST", "/api/servers/1/backups", "{}");
        int backupId = Json.readInt(createBackup.body(), "id", -1);
        HttpResponse<byte[]> download = sendBinaryRequest("GET", "/api/backups/" + backupId + "/download");

        assertEquals(200, sendConsole.statusCode());
        assertEquals(200, telemetry.statusCode());
        assertEquals("RUNNING", Json.readString(telemetry.body(), "operationalState"));
        assertEquals(200, stop.statusCode());
        assertEquals(201, createBackup.statusCode());
        assertTrue(download.body().length > 1);
        assertEquals('P', (char) download.body()[0]);
        assertEquals('K', (char) download.body()[1]);
    }

    private HttpResponse<String> sendRequest(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        if (body != null && !body.isEmpty()) {
            builder.header("Content-Type", "application/json");
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> sendBinaryRequest(String method, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private String getConsoleLogBody() {
        try {
            return sendRequest("GET", "/api/servers/1/console-log", null).body();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Properties loadConfiguredServer(Path propertiesFile) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(propertiesFile)) {
            properties.load(reader);
        }
        return properties;
    }

    private boolean canConnect(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private String stripPause(String script) {
        StringBuilder builder = new StringBuilder();
        for (String line : script.split("\\R")) {
            if (line.trim().equalsIgnoreCase("pause")) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(line);
        }
        builder.append(System.lineSeparator());
        return builder.toString();
    }

    private void restoreFile(Path path, byte[] content) throws IOException {
        if (path == null) {
            return;
        }
        if (content == null) {
            Files.deleteIfExists(path);
            return;
        }
        Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private void waitUntil(BooleanSupplier condition, Duration timeout, String failureMessage) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(250L);
        }
        fail(failureMessage);
    }
}
