package infra.http;

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
import infra.persistence.InMemoryBackupStore;
import infra.persistence.InMemoryServerStore;
import infra.persistence.InMemoryUserStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ApiServerTest {

    private HttpClient httpClient;
    private InMemoryServerStore serverStore;
    private ServerRuntimeState runtimeState;
    private ServerCatalogService serverCatalogService;
    private StartServerService startServerService;
    private StopServerService stopServerService;
    private ServerTelemetryService serverTelemetryService;
    private ApiServer apiServer;
    private Path tempMcDir;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        tempMcDir = createFakeMinecraftDirectory(Files.createTempDirectory("mc_test").resolve("server"));

        InMemoryUserStore userStore = new InMemoryUserStore();
        serverStore = new InMemoryServerStore();
        InMemoryBackupStore backupStore = new InMemoryBackupStore();
        runtimeState = new ServerRuntimeState();
        int testPort = findFreePort();

        ServerSupport serverSupport = new ServerSupport(serverStore, runtimeState);
        serverCatalogService = new ServerCatalogService(serverSupport);
        startServerService = new StartServerService(serverSupport);
        stopServerService = new StopServerService(serverSupport);
        RestartServerService restartServerService = new RestartServerService(
                serverCatalogService,
                startServerService,
                stopServerService
        );
        ServerConsoleService serverConsoleService = new ServerConsoleService(serverSupport);
        ServerFileService serverFileService = new ServerFileService(serverSupport);
        serverTelemetryService = new ServerTelemetryService(serverSupport);

        apiServer = new ApiServer(
                testPort,
                new AuthService(userStore),
                serverCatalogService,
                startServerService,
                stopServerService,
                restartServerService,
                serverConsoleService,
                serverFileService,
                serverTelemetryService,
                new BackupService(backupStore, serverStore)
        );
        apiServer.start();

        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + testPort;
    }

    @AfterEach
    void tearDown() {
        try {
            if (serverStore.findById(1L).isPresent() && !runtimeState.getRuntimeContexts().isEmpty()) {
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
    }

    @Test
    void authEndpoints_registerDuplicateAndLoginBehaveCorrectly() throws Exception {
        String body = "{\"username\":\"testuser\",\"password\":\"password123\"}";

        HttpResponse<String> firstRegister = sendRequest("POST", "/api/auth/register", body);
        HttpResponse<String> duplicateRegister = sendRequest("POST", "/api/auth/register", body);
        HttpResponse<String> login = sendRequest("POST", "/api/auth/login", body);
        HttpResponse<String> wrongPassword = sendRequest(
                "POST",
                "/api/auth/login",
                "{\"username\":\"testuser\",\"password\":\"wrongpassword\"}"
        );

        assertEquals(200, firstRegister.statusCode());
        assertEquals("testuser", Json.readString(firstRegister.body(), "username"));
        assertFalse(Json.readString(firstRegister.body(), "token").isBlank());
        assertEquals(400, duplicateRegister.statusCode());
        assertEquals(200, login.statusCode());
        assertEquals("testuser", Json.readString(login.body(), "username"));
        assertEquals(401, wrongPassword.statusCode());
    }

    @Test
    void serverRoutes_coverCrudAndEditableFilesWithoutRunningMinecraft() throws Exception {
        Path alternateDirectory = createFakeMinecraftDirectory(tempMcDir.getParent().resolve("alternate"));
        HttpResponse<String> createResponse = sendRequest(
                "POST",
                "/api/servers",
                serverRequestBody("ManagerServer", tempMcDir)
        );
        long serverId = Json.readInt(createResponse.body(), "id", -1);

        HttpResponse<String> getResponse = sendRequest("GET", "/api/servers/" + serverId, null);
        HttpResponse<String> getDirectory = sendRequest("GET", "/api/servers/" + serverId + "/directory", null);
        HttpResponse<String> duplicateCreate = sendRequest(
                "POST",
                "/api/servers",
                serverRequestBody("SecondServer", alternateDirectory)
        );
        HttpResponse<String> updateDirectory = sendRequest(
                "POST",
                "/api/servers/" + serverId + "/directory",
                "{\"path\":" + Json.quote(alternateDirectory.toString()) + "}"
        );
        HttpResponse<String> readStartParameters = sendRequest("GET", "/api/servers/" + serverId + "/start-parameters", null);
        HttpResponse<String> updateStartParameters = sendRequest(
                "POST",
                "/api/servers/" + serverId + "/start-parameters",
                "{\"content\":\"-Xms512M -Xmx1536M -Dcodex.api=true\"}"
        );
        HttpResponse<String> readUpdatedStartParameters = sendRequest("GET", "/api/servers/" + serverId + "/start-parameters", null);
        HttpResponse<String> readProperties = sendRequest("GET", "/api/servers/" + serverId + "/files/server-properties", null);
        HttpResponse<String> updateProperties = sendRequest(
                "POST",
                "/api/servers/" + serverId + "/files/server-properties",
                "{\"content\":\"motd=Updated by ApiServerTest\\nwhite-list=true\\n\"}"
        );
        HttpResponse<String> readWhitelist = sendRequest("GET", "/api/servers/" + serverId + "/files/whitelist", null);
        HttpResponse<String> updateWhitelist = sendRequest(
                "POST",
                "/api/servers/" + serverId + "/files/whitelist",
                "{\"content\":\"[\\n  {\\\"uuid\\\":\\\"1\\\",\\\"name\\\":\\\"Codex\\\"}\\n]\\n\"}"
        );

        assertEquals(201, createResponse.statusCode());
        assertEquals("ManagerServer", Json.readString(createResponse.body(), "name"));
        assertEquals(1L, serverId);
        assertEquals(200, getResponse.statusCode());
        assertEquals("ManagerServer", Json.readString(getResponse.body(), "name"));
        assertEquals(200, getDirectory.statusCode());
        assertEquals(tempMcDir.toString(), Json.readString(getDirectory.body(), "minecraftDirectory"));
        assertEquals(400, duplicateCreate.statusCode());
        assertEquals(200, updateDirectory.statusCode());
        assertEquals(alternateDirectory.toString(), Json.readString(updateDirectory.body(), "minecraftDirectory"));
        assertEquals(200, readStartParameters.statusCode());
        assertTrue(Json.readString(readStartParameters.body(), "content").contains("-Xmx2G"));
        assertEquals(200, updateStartParameters.statusCode());
        assertEquals("-Xms512M -Xmx1536M -Dcodex.api=true", Json.readString(readUpdatedStartParameters.body(), "content"));
        assertEquals(200, readProperties.statusCode());
        assertTrue(Json.readString(readProperties.body(), "content").contains("server-port=25565"));
        assertEquals(200, updateProperties.statusCode());
        assertTrue(Files.readString(alternateDirectory.resolve("server.properties")).contains("Updated by ApiServerTest"));
        assertEquals(200, readWhitelist.statusCode());
        assertTrue(Json.readString(readWhitelist.body(), "content").contains("\"Steve\""));
        assertEquals(200, updateWhitelist.statusCode());
        assertTrue(Files.readString(alternateDirectory.resolve("whitelist.json")).contains("\"Codex\""));
    }

    @Test
    void serverRoutes_coverRunningServerTelemetryConsoleBackupsAndCurrentAuthBehavior() throws Exception {
        HttpResponse<String> createResponse = sendRequest(
                "POST",
                "/api/servers",
                serverRequestBody("RunningServer", tempMcDir)
        );
        long serverId = Json.readInt(createResponse.body(), "id", -1);
        Path latestLog = tempMcDir.resolve("logs").resolve("latest.log");

        HttpResponse<String> startResponse = sendRequest("POST", "/api/servers/" + serverId + "/start", "{}");

        assertEquals(200, startResponse.statusCode());
        waitUntil(
                () -> getConsoleLogBody(serverId).contains("Done (0.100s)!"),
                Duration.ofSeconds(10),
                "Fake server never became ready."
        );

        Files.writeString(
                latestLog,
                """
                [12:00:00] [Server thread/INFO]: Alex joined the game
                [12:01:00] [Server thread/INFO]: Steve joined the game
                [12:02:00] [Server thread/INFO]: Alex left the game
                """,
                StandardOpenOption.APPEND
        );

        HttpResponse<String> consoleResponse = sendRequest(
                "POST",
                "/api/servers/" + serverId + "/console",
                "{\"command\":\"list\"}"
        );

        assertEquals(200, consoleResponse.statusCode());
        waitUntil(
                () -> getConsoleLogBody(serverId).contains("There are 0 of a max of 20 players online:"),
                Duration.ofSeconds(5),
                "Console output never included the list command response."
        );

        HttpResponse<String> telemetryResponse = sendRequest("GET", "/api/servers/" + serverId + "/telemetry", null);
        HttpResponse<String> playersResponse = sendRequest("GET", "/api/servers/" + serverId + "/players", null);
        HttpResponse<String> stopResponse = sendRequest("POST", "/api/servers/" + serverId + "/stop", "{}");
        HttpResponse<String> backupsBeforeCreate = sendRequest("GET", "/api/servers/" + serverId + "/backups", null);
        HttpResponse<String> createBackup = sendRequest("POST", "/api/servers/" + serverId + "/backups", "{}");
        HttpResponse<String> backupsAfterCreate = sendRequest("GET", "/api/servers/" + serverId + "/backups", null);
        int backupId = Json.readInt(backupsAfterCreate.body(), "id", -1);
        HttpResponse<byte[]> downloadBackup = sendBinaryRequest("GET", "/api/backups/" + backupId + "/download");

        assertEquals(200, telemetryResponse.statusCode());
        assertEquals("RUNNING", Json.readString(telemetryResponse.body(), "operationalState"));
        assertEquals("1G", Json.readString(telemetryResponse.body(), "jvmInitialRam"));
        assertEquals("2G", Json.readString(telemetryResponse.body(), "jvmAllocatedRam"));
        assertFalse(Json.readString(telemetryResponse.body(), "packetBase64").isBlank());
        assertEquals(200, playersResponse.statusCode());
        assertTrue(playersResponse.body().contains("\"Steve\""));
        assertFalse(playersResponse.body().contains("\"Alex\""));
        assertEquals(200, stopResponse.statusCode());
        assertEquals("[]", backupsBeforeCreate.body());
        assertEquals(201, createBackup.statusCode());
        assertTrue(Json.readString(createBackup.body(), "filename").endsWith(".zip"));
        assertTrue(backupsAfterCreate.body().contains("\"serverId\":1"));
        assertTrue(backupId > 0);
        assertEquals(200, downloadBackup.statusCode());
        assertTrue(downloadBackup.body().length > 1);
        assertEquals('P', (char) downloadBackup.body()[0]);
        assertEquals('K', (char) downloadBackup.body()[1]);
    }

    @Test
    void invalidAndUnknownRoutes_returnExpectedStatusCodes() throws Exception {
        HttpResponse<String> removedListRoute = sendRequest("GET", "/api/servers", null);
        HttpResponse<String> invalidServerId = sendRequest("GET", "/api/servers/not-a-number", null);
        HttpResponse<String> missingServer = sendRequest("GET", "/api/servers/999", null);
        HttpResponse<String> unknownRoute = sendRequest("GET", "/api/unknown", null);

        assertEquals(404, removedListRoute.statusCode());
        assertEquals(400, invalidServerId.statusCode());
        assertEquals(404, missingServer.statusCode());
        assertEquals(404, unknownRoute.statusCode());
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

    private String getConsoleLogBody(long serverId) {
        try {
            return sendRequest("GET", "/api/servers/" + serverId + "/console-log", null).body();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String serverRequestBody(String name, Path minecraftDirectory) {
        return "{"
                + "\"name\":" + Json.quote(name) + ","
                + "\"host\":\"localhost\","
                + "\"port\":25565,"
                + "\"minecraftDirectory\":" + Json.quote(minecraftDirectory.toString())
                + "}";
    }

    private Path createFakeMinecraftDirectory(Path directory) throws IOException {
        Files.createDirectories(directory.resolve("logs"));
        Files.createDirectories(directory.resolve("world"));
        Files.writeString(directory.resolve("server.properties"), "server-port=25565\nmotd=Fake server\n");
        Files.writeString(directory.resolve("whitelist.json"), "[\n  {\"uuid\":\"abc\",\"name\":\"Steve\"}\n]\n");
        Files.writeString(directory.resolve("world").resolve("level.dat"), "fake-world-data");
        Files.writeString(directory.resolve("logs").resolve("latest.log"), "");
        Files.writeString(directory.resolve("start.bat"), fakeStartScript());
        return directory;
    }

    private String fakeStartScript() {
        return "@echo off\r\n"
                + "REM java -Xms1G -Xmx2G -jar server.jar nogui\r\n"
                + "java -Xms1G -Xmx2G -cp \"" + compiledTestClasspath() + "\" app.server.FakeMinecraftProcessMain\r\n";
    }

    private String compiledTestClasspath() {
        return Path.of("server", "out").toAbsolutePath().normalize().toString();
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void waitUntil(BooleanSupplier condition, Duration timeout, String failureMessage) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200L);
        }
        fail(failureMessage);
    }
}
