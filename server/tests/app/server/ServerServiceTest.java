package app.server;

import app.server.dto.ServerRequest;
import domain.server.ManagedServer;
import domain.server.ServerStatus;
import infra.persistence.InMemoryServerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class ServerServiceTest {

    private InMemoryServerStore serverStore;
    private ServerSupport serverSupport;
    private ServerCatalogService serverCatalogService;
    private Path tempMcDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        tempMcDir = tempDir.resolve("mc");
        Files.createDirectories(tempMcDir);
        Files.writeString(tempMcDir.resolve("server.properties"), "server-port=25565\n");
        serverStore = new InMemoryServerStore();
        serverSupport = new ServerSupport(serverStore, new ServerRuntimeState());
        serverCatalogService = new ServerCatalogService(serverSupport);
    }

    @Test
    void getServer_withExistingId_returnsServer() {
        ManagedServer created = createServer("TestServer");

        Optional<ManagedServer> result = serverCatalogService.getServer(created.getId());

        assertTrue(result.isPresent());
        assertEquals("TestServer", result.get().getName());
    }

    @Test
    void getServer_withNonExistingId_returnsEmpty() {
        Optional<ManagedServer> result = serverCatalogService.getServer(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void createServer_withValidRequest_returnsServer() {
        ServerRequest request = new ServerRequest(
            "MyServer", "localhost", 25565, "password", 25575,
            "motd=Test", "/backups", tempMcDir.toString()
        );

        Optional<ManagedServer> result = serverCatalogService.createServer(request, 1L);

        assertTrue(result.isPresent());
        assertEquals("MyServer", result.get().getName());
        assertEquals("localhost", result.get().getHost());
        assertEquals(25565, result.get().getPort());
        assertEquals(ServerStatus.STOPPED, result.get().getStatus());
        assertEquals(1L, result.get().getOwnerId());
    }

    @Test
    void createServer_whenServerAlreadyExists_returnsEmpty() {
        createServer("ExistingServer");

        ServerRequest request = new ServerRequest(
            "SecondServer", "localhost", 25565, "password", 25575,
            "motd=Test", "/backups", tempMcDir.toString()
        );

        assertTrue(serverCatalogService.createServer(request, 1L).isEmpty());
    }

    @Test
    void createServer_withNullRequest_returnsEmpty() {
        Optional<ManagedServer> result = serverCatalogService.createServer(null, 1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void createServer_withBlankName_returnsEmpty() {
        ServerRequest request = new ServerRequest(
            "", "localhost", 25565, "pass", 25575, null, null, null
        );
        assertTrue(serverCatalogService.createServer(request, 1L).isEmpty());

        request = new ServerRequest("   ", "localhost", 25565, "pass", 25575, null, null, null);
        assertTrue(serverCatalogService.createServer(request, 1L).isEmpty());

        request = new ServerRequest(null, "localhost", 25565, "pass", 25575, null, null, null);
        assertTrue(serverCatalogService.createServer(request, 1L).isEmpty());
    }

    @Test
    void createServer_withBlankHost_returnsEmpty() {
        ServerRequest request = new ServerRequest(
            "Server", "", 25565, "pass", 25575, null, null, null
        );
        assertTrue(serverCatalogService.createServer(request, 1L).isEmpty());

        request = new ServerRequest("Server", "   ", 25565, "pass", 25575, null, null, null);
        assertTrue(serverCatalogService.createServer(request, 1L).isEmpty());

        request = new ServerRequest("Server", null, 25565, "pass", 25575, null, null, null);
        assertTrue(serverCatalogService.createServer(request, 1L).isEmpty());
    }

    @Test
    void createServer_withInvalidPort_returnsEmpty() {
        ServerRequest request = new ServerRequest(
            "Server", "localhost", 0, "pass", 25575, null, null, null
        );
        assertTrue(serverCatalogService.createServer(request, 1L).isEmpty());

        request = new ServerRequest("Server", "localhost", -1, "pass", 25575, null, null, null);
        assertTrue(serverCatalogService.createServer(request, 1L).isEmpty());
    }

    @Test
    void createServer_withValidPort_returnsServer() {
        ServerRequest request = new ServerRequest(
            "Server", "localhost", 1, "pass", 25575, null, null, null
        );
        assertTrue(serverCatalogService.createServer(request, 1L).isPresent());
    }

    @Test
    void createServer_withNullOptionalFields_setsToEmptyString() {
        ServerRequest request = new ServerRequest(
            "Server", "localhost", 25565, null, 25575, null, null, null
        );

        ManagedServer result = serverCatalogService.createServer(request, 1L).orElseThrow();
        assertEquals("", result.getRconPassword());
        assertEquals("", result.getServerProperties());
        assertEquals("", result.getBackupPath());
    }

    @Test
    void createServer_nameIsTrimmed() {
        ServerRequest request = new ServerRequest(
            "  MyServer  ", "localhost", 25565, "pass", 25575, null, null, null
        );

        ManagedServer result = serverCatalogService.createServer(request, 1L).orElseThrow();
        assertEquals("MyServer", result.getName());
    }

    @Test
    void createServer_withoutMinecraftDirectory_startsInStartupState() {
        ServerRequest request = new ServerRequest(
            "MyServer", "localhost", 25565, "pass", 25575, null, null, "   "
        );

        ManagedServer result = serverCatalogService.createServer(request, 1L).orElseThrow();

        assertEquals(ServerStatus.STARTUP, result.getStatus());
        assertEquals("", result.getMinecraftDirectory());
    }

    @Test
    void createServer_withMinecraftDirectoryAndNoProperties_readsServerPropertiesFromDisk() {
        ServerRequest request = new ServerRequest(
            "MyServer", "localhost", 25565, "pass", 25575, null, null, tempMcDir.toString()
        );

        ManagedServer result = serverCatalogService.createServer(request, 1L).orElseThrow();

        assertTrue(result.getServerProperties().contains("server-port=25565"));
    }

    @Test
    void updateMinecraftDirectory_updatesDirectoryPropertiesAndStatus() throws IOException {
        ManagedServer created = createServer("TestServer");
        Path alternateDir = tempMcDir.getParent().resolve("alternate");
        Files.createDirectories(alternateDir);
        Files.writeString(alternateDir.resolve("server.properties"), "motd=Alternate\n");

        ManagedServer updated = serverCatalogService.updateMinecraftDirectory(created.getId(), alternateDir.toString());

        assertEquals(alternateDir.toAbsolutePath().toString(), updated.getMinecraftDirectory());
        assertEquals(ServerStatus.STOPPED, updated.getStatus());
        assertTrue(updated.getServerProperties().contains("motd=Alternate"));
    }

    @Test
    void updateMinecraftDirectory_whileRunning_throwsException() {
        ManagedServer created = createServer("TestServer");
        created.setStatus(ServerStatus.RUNNING);
        serverStore.save(created);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> serverCatalogService.updateMinecraftDirectory(created.getId(), tempMcDir.toString())
        );

        assertEquals("Stop the server before changing the Minecraft directory.", exception.getMessage());
    }

    @Test
    void readMinecraftDirectory_returnsPersistedDirectory() {
        ManagedServer created = createServer("TestServer");

        String minecraftDirectory = serverCatalogService.readMinecraftDirectory(created.getId());

        assertEquals(tempMcDir.toAbsolutePath().toString(), minecraftDirectory);
    }

    private ManagedServer createServer(String name) {
        ServerRequest request = new ServerRequest(
            name, "localhost", 25565, "password", 25575,
            "motd=Test", "/backups", tempMcDir.toString()
        );
        return serverCatalogService.createServer(request, 1L).orElseThrow();
    }
}
