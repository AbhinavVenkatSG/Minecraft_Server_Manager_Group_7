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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerFileServiceTest {

    private InMemoryServerStore serverStore;
    private ServerCatalogService serverCatalogService;
    private ServerFileService serverFileService;
    private Path minecraftDirectory;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        minecraftDirectory = tempDir.resolve("mc");
        Files.createDirectories(minecraftDirectory.resolve("world"));
        Files.writeString(minecraftDirectory.resolve("server.properties"), "motd=Original\n");
        Files.writeString(minecraftDirectory.resolve("whitelist.json"), "[{\"name\":\"Steve\"}]\n");
        Files.writeString(
                minecraftDirectory.resolve("start.bat"),
                """
                @echo off
                REM java -Xms1G -Xmx2G -jar server.jar nogui
                echo fake
                """
        );

        serverStore = new InMemoryServerStore();
        ServerSupport serverSupport = new ServerSupport(serverStore, new ServerRuntimeState());
        serverCatalogService = new ServerCatalogService(serverSupport);
        serverFileService = new ServerFileService(serverSupport);
    }

    @Test
    void serverProperties_roundTripThroughDiskAndServerState() throws IOException {
        ManagedServer server = createServer();

        String original = serverFileService.readServerProperties(server.getId());
        ManagedServer updated = serverFileService.writeServerProperties(server.getId(), "motd=Updated\n");

        assertEquals("motd=Original\n", original);
        assertEquals("motd=Updated\n", Files.readString(minecraftDirectory.resolve("server.properties")));
        assertEquals("motd=Updated\n", updated.getServerProperties());
    }

    @Test
    void whitelist_roundTripThroughDisk() throws IOException {
        ManagedServer server = createServer();

        String original = serverFileService.readWhitelist(server.getId());
        serverFileService.writeWhitelist(server.getId(), "[{\"name\":\"Codex\"}]\n");

        assertTrue(original.contains("Steve"));
        assertTrue(Files.readString(minecraftDirectory.resolve("whitelist.json")).contains("Codex"));
    }

    @Test
    void startParameters_roundTripThroughStartScript() throws IOException {
        ManagedServer server = createServer();

        String original = serverFileService.readStartParameters(server.getId());
        serverFileService.writeStartParameters(server.getId(), "-Xms512M -Xmx1536M -Dfile.test=true");
        String updated = serverFileService.readStartParameters(server.getId());

        assertEquals("-Xms1G -Xmx2G", original);
        assertEquals("-Xms512M -Xmx1536M -Dfile.test=true", updated);
        assertTrue(Files.readString(minecraftDirectory.resolve("start.bat")).contains("-Dfile.test=true"));
    }

    @Test
    void writeOperations_requireStoppedServer() {
        ManagedServer server = createServer();
        server.setStatus(ServerStatus.RUNNING);
        serverStore.save(server);

        assertThrows(IllegalStateException.class, () -> serverFileService.writeServerProperties(server.getId(), "motd=Running\n"));
        assertThrows(IllegalStateException.class, () -> serverFileService.writeWhitelist(server.getId(), "[]"));
        assertThrows(IllegalStateException.class, () -> serverFileService.writeStartParameters(server.getId(), "-Xmx4G"));
    }

    @Test
    void readStartParameters_withoutJavaLaunchLine_throwsException() throws IOException {
        ManagedServer server = createServer();
        Files.writeString(minecraftDirectory.resolve("start.bat"), "@echo off\necho no java line here\n");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> serverFileService.readStartParameters(server.getId())
        );

        assertEquals("Unable to find Java launch arguments in the start script.", exception.getMessage());
    }

    private ManagedServer createServer() {
        return serverCatalogService.createServer(
                new ServerRequest(
                        "TestServer",
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
}
