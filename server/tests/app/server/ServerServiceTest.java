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
import java.util.List;
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
    void listServers_withNoServers_returnsEmptyList() {
        List<ManagedServer> result = serverCatalogService.listServers();
        assertTrue(result.isEmpty());
    }

    @Test
    void listServers_afterCreatingServers_returnsAllServers() {
        createServer("Server1");
        createServer("Server2");

        List<ManagedServer> result = serverCatalogService.listServers();

        assertEquals(2, result.size());
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

    private ManagedServer createServer(String name) {
        ServerRequest request = new ServerRequest(
            name, "localhost", 25565, "password", 25575,
            "motd=Test", "/backups", tempMcDir.toString()
        );
        return serverCatalogService.createServer(request, 1L).orElseThrow();
    }
}
