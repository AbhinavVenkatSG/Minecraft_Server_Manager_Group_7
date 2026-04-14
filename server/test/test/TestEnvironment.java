package test;

import app.auth.AuthService;
import app.auth.dto.LoginRequest;
import app.auth.dto.RegisterRequest;
import app.backup.BackupService;
import app.server.RestartServerService;
import app.server.ServerCatalogService;
import app.server.ServerConsoleService;
import app.server.ServerFileService;
import app.server.ServerRuntimeState;
import app.server.ServerSupport;
import app.server.StartServerService;
import app.server.StopServerService;
import app.server.dto.ServerRequest;
import domain.server.ManagedServer;
import infra.persistence.InMemoryBackupStore;
import infra.persistence.InMemoryServerStore;
import infra.persistence.InMemoryUserStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class TestEnvironment implements AutoCloseable {
    private static final String DEFAULT_SERVER_PROPERTIES = "motd=Test Server%nview-distance=10%n";
    private static final String DEFAULT_START_SCRIPT = "@echo off\r\n"
            + "echo BOOTED\r\n"
            + ":loop\r\n"
            + "set \"cmd=\"\r\n"
            + "set /p cmd=\r\n"
            + "if /I \"%cmd%\"==\"stop\" goto stop\r\n"
            + "if not \"%cmd%\"==\"\" echo CMD:%cmd%\r\n"
            + "goto loop\r\n"
            + ":stop\r\n"
            + "echo STOPPING\r\n";

    private final Path root;
    private final InMemoryServerStore serverStore = new InMemoryServerStore();
    private final InMemoryBackupStore backupStore = new InMemoryBackupStore();
    private final InMemoryUserStore userStore = new InMemoryUserStore();
    private final ServerRuntimeState runtimeState = new ServerRuntimeState();
    private final ServerSupport support = new ServerSupport(serverStore, runtimeState);

    public final ServerCatalogService catalogService = new ServerCatalogService(support);
    public final StartServerService startService = new StartServerService(support);
    public final StopServerService stopService = new StopServerService(support);
    public final RestartServerService restartService = new RestartServerService(catalogService, startService, stopService);
    public final ServerConsoleService consoleService = new ServerConsoleService(support);
    public final ServerFileService fileService = new ServerFileService(support);
    public final BackupService backupService = new BackupService(backupStore, serverStore);
    public final AuthService authService = new AuthService(userStore);

    public TestEnvironment() throws IOException {
        this.root = Files.createTempDirectory("mc-manager-tests-");
    }

    public Path getRoot() {
        return root;
    }

    public ServerSupport getSupport() {
        return support;
    }

    public ServerRuntimeState getRuntimeState() {
        return runtimeState;
    }

    public Path createMinecraftDirectory() throws IOException {
        return createMinecraftDirectory(DEFAULT_START_SCRIPT);
    }

    public Path createMinecraftDirectory(String startScriptContent) throws IOException {
        Path directory = root.resolve("mc-" + UUID.randomUUID());
        Files.createDirectories(directory.resolve("world"));
        Files.createDirectories(directory.resolve("logs"));
        Files.writeString(directory.resolve("server.properties"), DEFAULT_SERVER_PROPERTIES.formatted(), StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("whitelist.json"), "[\"Alex\"]%n".formatted(), StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("world").resolve("level.dat"), "level-data", StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("logs").resolve("latest.log"), "", StandardCharsets.UTF_8);
        if (startScriptContent != null) {
            Files.writeString(directory.resolve("start.bat"), startScriptContent, StandardCharsets.UTF_8);
        }
        return directory;
    }

    public ManagedServer createServer(Path minecraftDirectory) {
        return catalogService.createServer(
                new ServerRequest(
                        "Test Server",
                        "localhost",
                        25565,
                        "secret",
                        25575,
                        "",
                        "",
                        minecraftDirectory.toString()
                ),
                1L
        ).orElseThrow(() -> new AssertionError("Server should have been created."));
    }

    public void waitForCondition(BooleanSupplier condition, Duration timeout, String failureMessage) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError(failureMessage);
    }

    public void waitForConsoleLine(long serverId, String expectedLine) throws Exception {
        waitForCondition(
                () -> consoleService.getRecentConsoleLines(serverId).stream().anyMatch(line -> line.contains(expectedLine)),
                Duration.ofSeconds(5),
                "Timed out waiting for console line: " + expectedLine
        );
    }

    public void appendLatestLog(Path minecraftDirectory, String content) throws IOException {
        Files.writeString(
                minecraftDirectory.resolve("logs").resolve("latest.log"),
                content,
                StandardCharsets.UTF_8
        );
    }

    public void registerAndLogin(String username, String password) {
        Assertions.assertTrue(
                authService.register(new RegisterRequest(username, password)).isPresent(),
                "Registration should succeed."
        );
        Assertions.assertTrue(
                authService.login(new LoginRequest(username, password)).isPresent(),
                "Login should succeed."
        );
    }

    @Override
    public void close() throws Exception {
        for (ServerRuntimeState.RuntimeContext context : runtimeState.getRuntimeContexts().values()) {
            try {
                context.getProcess().destroyForcibly();
                context.getProcess().waitFor();
            } catch (Exception ignored) {
            }
        }
        runtimeState.getRuntimeContexts().clear();
        deleteRecursively(root);
    }

    private void deleteRecursively(Path target) throws IOException {
        if (target == null || !Files.exists(target)) {
            return;
        }

        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
