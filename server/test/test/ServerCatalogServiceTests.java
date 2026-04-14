package test;

import app.server.ServerCatalogService;
import app.server.ServerRuntimeState;
import app.server.ServerSupport;
import app.server.dto.ServerRequest;
import domain.server.ManagedServer;
import domain.server.ServerStatus;
import infra.persistence.FileServerStore;
import infra.persistence.InMemoryServerStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public final class ServerCatalogServiceTests {
    private ServerCatalogServiceTests() {
    }

    public static void normalizesOperationalStateOnStartup() throws Exception {
        InMemoryServerStore serverStore = new InMemoryServerStore();
        ServerRuntimeState runtimeState = new ServerRuntimeState();
        ManagedServer persisted = new ManagedServer(
                0L,
                "Persisted",
                "localhost",
                25565,
                "",
                25575,
                ServerStatus.RUNNING,
                "",
                "",
                "",
                1L,
                LocalDateTime.now(),
                null
        );
        serverStore.save(persisted);

        ServerCatalogService catalogService = new ServerCatalogService(new ServerSupport(serverStore, runtimeState));
        ManagedServer normalized = catalogService.getServer(persisted.getId())
                .orElseThrow(() -> new AssertionError("Server should still exist after normalization."));

        Assertions.assertEquals(ServerStatus.STARTUP, normalized.getStatus(), "REQ-SVR-001: startup normalization should reset invalid running state.");
    }

    public static void persistsMinecraftDirectoryConfiguration() throws Exception {
        Path dataRoot = Files.createTempDirectory("catalog-service-tests-");
        try {
            Path minecraftDirectory = Files.createDirectories(dataRoot.resolve("minecraft"));
            Files.writeString(minecraftDirectory.resolve("server.properties"), "online-mode=true");
            Files.createDirectories(minecraftDirectory.resolve("world"));

            FileServerStore serverStore = new FileServerStore(dataRoot);
            ServerCatalogService catalogService = new ServerCatalogService(new ServerSupport(serverStore, new ServerRuntimeState()));

            var created = catalogService.createServer(
                    new ServerRequest(
                            "Realm",
                            "localhost",
                            25565,
                            "",
                            25575,
                            "",
                            "",
                            minecraftDirectory.toString()
                    ),
                    7L
            );

            Assertions.assertTrue(created.isPresent(), "REQ-SVR-101: valid Minecraft directories should be accepted.");
            Assertions.assertEquals(ServerStatus.STOPPED, created.get().getStatus(), "Configured servers should start in the stopped state.");

            FileServerStore reloadedStore = new FileServerStore(dataRoot);
            ServerCatalogService reloadedCatalog = new ServerCatalogService(new ServerSupport(reloadedStore, new ServerRuntimeState()));
            ManagedServer reloaded = reloadedCatalog.listServers().get(0);

            Assertions.assertEquals(
                    minecraftDirectory.toAbsolutePath().normalize().toString(),
                    reloaded.getMinecraftDirectory(),
                    "REQ-SVR-101: the Minecraft directory should persist to storage."
            );
            Assertions.assertContains(reloaded.getServerProperties(), "online-mode=true", "Server properties should be loaded from disk.");
        } finally {
            if (Files.exists(dataRoot)) {
                try (var paths = Files.walk(dataRoot)) {
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
}
