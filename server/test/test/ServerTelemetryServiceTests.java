package test;

import app.server.ServerCatalogService;
import app.server.ServerRuntimeState;
import app.server.ServerSupport;
import app.server.ServerTelemetryService;
import app.server.dto.ServerRequest;
import core.protocol.Packet;
import core.protocol.PacketType;
import domain.server.ManagedServer;
import infra.persistence.InMemoryServerStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ServerTelemetryServiceTests {
    private ServerTelemetryServiceTests() {
    }

    public static void collectsTelemetrySnapshotForServerRequirements() throws Exception {
        Path root = Files.createTempDirectory("telemetry-service-tests-");
        ServerTelemetryService telemetryService = null;
        try {
            Path minecraftDirectory = Files.createDirectories(root.resolve("minecraft"));
            Files.createDirectories(minecraftDirectory.resolve("world"));
            Files.createDirectories(minecraftDirectory.resolve("logs"));
            Files.writeString(minecraftDirectory.resolve("world").resolve("level.dat"), "level", StandardCharsets.UTF_8);
            Files.writeString(minecraftDirectory.resolve("server.properties"), "motd=telemetry", StandardCharsets.UTF_8);
            Files.writeString(
                    minecraftDirectory.resolve("start.bat"),
                    "@echo off\r\njava -Xms2G -Xmx4G -jar server.jar nogui\r\n",
                    StandardCharsets.UTF_8
            );
            Files.writeString(minecraftDirectory.resolve("logs").resolve("latest.log"), "placeholder", StandardCharsets.UTF_8);

            InMemoryServerStore serverStore = new InMemoryServerStore();
            ServerRuntimeState runtimeState = new ServerRuntimeState();
            ControlledTelemetrySupport support = new ControlledTelemetrySupport(serverStore, runtimeState);
            ServerCatalogService catalogService = new ServerCatalogService(support);
            ManagedServer server = catalogService.createServer(
                    new ServerRequest("Telemetry", "localhost", 25565, "", 25575, "", "", minecraftDirectory.toString()),
                    1L
            ).orElseThrow(() -> new AssertionError("Server should be created for telemetry test."));

            runtimeState.getRuntimeContexts().put(server.getId(), new ServerRuntimeState.RuntimeContext(new FakeAliveProcess()));
            telemetryService = new ServerTelemetryService(support);

            var snapshot = telemetryService.getTelemetry(server.getId());
            Assertions.assertEquals("4G", snapshot.getJvmAllocatedRam(), "REQ-SVR-302: telemetry should include JVM max allocation.");
            Assertions.assertEquals("2G", snapshot.getJvmInitialRam(), "REQ-SVR-302: telemetry should include JVM initial allocation.");
            Assertions.assertEquals(268_435_456L, snapshot.getMinecraftProcessMemoryBytes(), "REQ-SVR-302: telemetry should include JVM RAM usage.");
            Assertions.assertEquals(17.5d, snapshot.getMinecraftProcessCpuLoadPercent(), "REQ-SVR-303: telemetry should include process CPU usage.");
            Assertions.assertEquals(4_096L, snapshot.getMinecraftDirectorySizeBytes(), "REQ-SVR-304: telemetry should include Minecraft directory size.");
            Assertions.assertEquals(1_000_000L, snapshot.getDriveTotalBytes(), "REQ-SVR-305: telemetry should include total drive capacity.");
            Assertions.assertEquals(400_000L, snapshot.getDriveUsableBytes(), "REQ-SVR-305: telemetry should include remaining drive capacity.");
            Assertions.assertEquals(2, snapshot.getPlayerCount(), "REQ-SVR-307: telemetry should include player count.");
            Assertions.assertEquals(List.of("Alex", "Steve"), snapshot.getOnlinePlayers(), "REQ-SVR-307: telemetry should include the current player list.");
            Assertions.assertEquals(2, snapshot.getNewLogLines().size(), "REQ-SVR-306: telemetry should include new latest.log lines.");

            Packet packet = Packet.fromBytes(Base64.getDecoder().decode(snapshot.getPacketBase64()));
            Assertions.assertEquals(PacketType.TELEMETRY, packet.getType(), "Telemetry snapshots should serialize as telemetry packets.");
            Assertions.assertTrue(packet.isValid(), "Telemetry packets should have a valid CRC.");
            Assertions.assertContains(packet.getPayload(), "playerCount=2", "Telemetry packets should include player count.");
            Assertions.assertContains(packet.getPayload(), "minecraftDirectorySizeBytes=4096", "Telemetry packets should include directory size.");

            Assertions.assertTrue(telemetryService.getLatestTelemetry(server.getId()).isPresent(), "Telemetry service should cache the latest snapshot.");
            Assertions.assertEquals(1, telemetryService.getTelemetryHistory(server.getId()).size(), "Telemetry history should record collected snapshots.");
        } finally {
            if (telemetryService != null) {
                telemetryService.shutdown();
            }
            if (Files.exists(root)) {
                try (var paths = Files.walk(root)) {
                    paths.sorted((left, right) -> right.compareTo(left))
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (Exception exception) {
                                    throw new RuntimeException(exception);
                                }
                            });
                }
            }
        }
    }

    private static final class ControlledTelemetrySupport extends ServerSupport {
        private ControlledTelemetrySupport(InMemoryServerStore serverStore, ServerRuntimeState runtimeState) {
            super(serverStore, runtimeState);
        }

        @Override
        public long computeDirectorySize(Path root) {
            return 4_096L;
        }

        @Override
        public long[] getDriveMetrics(Path path) {
            return new long[] {1_000_000L, 400_000L};
        }

        @Override
        public List<String> readNewLogLines(long serverId, Path latestLogPath) {
            return List.of(
                    "[00:00:01] [Server thread/INFO]: Alex joined the game",
                    "[00:00:10] [Server thread/INFO]: Steve joined the game"
            );
        }

        @Override
        public List<String> readOnlinePlayers(Path latestLogPath) {
            return List.of("Alex", "Steve");
        }

        @Override
        public double readProcessCpuLoad(ServerRuntimeState.RuntimeContext context) {
            return 17.5d;
        }

        @Override
        public long readProcessMemoryBytes(long pid) {
            return 268_435_456L;
        }
    }

    private static final class FakeAliveProcess extends Process {
        private final ByteArrayOutputStream stdin = new ByteArrayOutputStream();
        private final InputStream stdout = new ByteArrayInputStream(new byte[0]);
        private volatile boolean alive = true;

        @Override
        public OutputStream getOutputStream() {
            return stdin;
        }

        @Override
        public InputStream getInputStream() {
            return stdout;
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            alive = false;
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            alive = false;
            return true;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("Process is still running.");
            }
            return 0;
        }

        @Override
        public void destroy() {
            alive = false;
        }

        @Override
        public Process destroyForcibly() {
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public long pid() {
            return 4242L;
        }
    }
}
