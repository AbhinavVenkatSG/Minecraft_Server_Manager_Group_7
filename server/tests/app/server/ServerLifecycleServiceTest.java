package app.server;

import app.server.dto.ServerRequest;
import domain.server.ManagedServer;
import domain.server.ServerStatus;
import domain.server.TelemetrySnapshot;
import infra.persistence.InMemoryServerStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ServerLifecycleServiceTest {

    private InMemoryServerStore serverStore;
    private ServerRuntimeState runtimeState;
    private ServerCatalogService serverCatalogService;
    private StartServerService startServerService;
    private StopServerService stopServerService;
    private RestartServerService restartServerService;
    private ServerConsoleService serverConsoleService;
    private ServerTelemetryService serverTelemetryService;
    private Path minecraftDirectory;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        minecraftDirectory = tempDir.resolve("mc");
        Files.createDirectories(minecraftDirectory.resolve("logs"));
        Files.createDirectories(minecraftDirectory.resolve("world"));
        Files.writeString(minecraftDirectory.resolve("server.properties"), "server-port=25565\n");
        Files.writeString(minecraftDirectory.resolve("logs").resolve("latest.log"), "");
        Files.writeString(minecraftDirectory.resolve("world").resolve("level.dat"), "world-data");
        Files.writeString(minecraftDirectory.resolve("start.bat"), fakeStartScript());

        serverStore = new InMemoryServerStore();
        runtimeState = new ServerRuntimeState();
        ServerSupport serverSupport = new ServerSupport(serverStore, runtimeState);
        serverCatalogService = new ServerCatalogService(serverSupport);
        startServerService = new StartServerService(serverSupport);
        stopServerService = new StopServerService(serverSupport);
        restartServerService = new RestartServerService(serverCatalogService, startServerService, stopServerService);
        serverConsoleService = new ServerConsoleService(serverSupport);
        serverTelemetryService = new ServerTelemetryService(serverSupport);
    }

    @AfterEach
    void tearDown() {
        try {
            if (!runtimeState.getRuntimeContexts().isEmpty()) {
                stopServerService.stopServer(1L);
            }
        } catch (Exception ignored) {
        }
        serverTelemetryService.shutdown();
    }

    @Test
    void lifecycleConsoleAndTelemetryWorkAgainstFakeServerProcess() throws Exception {
        ManagedServer server = createServer();

        ManagedServer started = startServerService.startServer(server.getId()).orElseThrow();
        waitUntil(
                () -> serverConsoleService.getRecentConsoleLines(server.getId()).stream().anyMatch(line -> line.contains("Done (0.100s)!")),
                Duration.ofSeconds(10),
                "Fake server never became ready."
        );

        serverConsoleService.sendConsoleCommand(server.getId(), "list");
        waitUntil(
                () -> serverConsoleService.getRecentConsoleLines(server.getId()).stream().anyMatch(line -> line.contains("There are 0 of a max of 20 players online:")),
                Duration.ofSeconds(5),
                "Console output never included the list response."
        );

        Files.writeString(
                minecraftDirectory.resolve("logs").resolve("latest.log"),
                """
                [12:00:00] [Server thread/INFO]: Alex joined the game
                [12:01:00] [Server thread/INFO]: Steve joined the game
                [12:02:00] [Server thread/INFO]: Alex left the game
                """,
                StandardOpenOption.APPEND
        );

        TelemetrySnapshot snapshot = serverTelemetryService.getTelemetry(server.getId());

        assertEquals(ServerStatus.RUNNING, started.getStatus());
        assertEquals(ServerStatus.RUNNING, snapshot.getOperationalState());
        assertEquals(1, snapshot.getPlayerCount());
        assertEquals("Steve", snapshot.getOnlinePlayers().get(0));
        assertFalse(snapshot.getPacketBase64().isBlank());
        assertTrue(snapshot.getMinecraftDirectorySizeBytes() > 0L);
        assertFalse(serverTelemetryService.getTelemetryHistory(server.getId()).isEmpty());

        ManagedServer restarted = restartServerService.restartServer(server.getId()).orElseThrow();
        waitUntil(
                () -> serverConsoleService.getRecentConsoleLines(server.getId()).stream().anyMatch(line -> line.contains("Done (0.100s)!")),
                Duration.ofSeconds(10),
                "Fake server did not become ready after restart."
        );
        assertEquals(ServerStatus.RUNNING, restarted.getStatus());

        ManagedServer stopped = stopServerService.stopServer(server.getId()).orElseThrow();

        assertEquals(ServerStatus.STOPPED, stopped.getStatus());
        assertTrue(runtimeState.getRuntimeContexts().isEmpty());
    }

    @Test
    void startingAlreadyRunningServer_throwsException() throws Exception {
        ManagedServer server = createServer();
        startServerService.startServer(server.getId()).orElseThrow();

        waitUntil(
                () -> serverConsoleService.getRecentConsoleLines(server.getId()).stream().anyMatch(line -> line.contains("Done (0.100s)!")),
                Duration.ofSeconds(10),
                "Fake server never became ready."
        );

        assertThrows(IllegalStateException.class, () -> startServerService.startServer(server.getId()));
        stopServerService.stopServer(server.getId());
    }

    @Test
    void stopAndConsoleCommandsRequireRunningServer() {
        ManagedServer server = createServer();

        assertThrows(IllegalStateException.class, () -> stopServerService.stopServer(server.getId()));
        assertThrows(IllegalArgumentException.class, () -> serverConsoleService.sendConsoleCommand(server.getId(), "   "));
        assertThrows(IllegalStateException.class, () -> serverConsoleService.sendConsoleCommand(server.getId(), "list"));
        assertTrue(serverConsoleService.getRecentConsoleLines(server.getId()).isEmpty());
    }

    private ManagedServer createServer() {
        return serverCatalogService.createServer(
                new ServerRequest(
                        "LifecycleServer",
                        "localhost",
                        25565,
                        "",
                        25575,
                        null,
                        "",
                        minecraftDirectory.toString()
                ),
                1L
        ).orElseThrow();
    }

    private String fakeStartScript() {
        return "@echo off\r\n"
                + "REM java -Xms1G -Xmx2G -jar server.jar nogui\r\n"
                + "java -Xms1G -Xmx2G -cp \"" + compiledTestClasspath() + "\" app.server.FakeMinecraftProcessMain\r\n";
    }

    private String compiledTestClasspath() {
        return Path.of("server", "out").toAbsolutePath().normalize().toString();
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
