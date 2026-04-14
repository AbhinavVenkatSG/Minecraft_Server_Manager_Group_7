package app.backup;

import app.server.ServerCatalogService;
import app.server.ServerRuntimeState;
import app.server.ServerSupport;
import app.server.dto.ServerRequest;
import domain.backup.Backup;
import domain.server.ManagedServer;
import infra.persistence.InMemoryBackupStore;
import infra.persistence.InMemoryServerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {

    private InMemoryBackupStore backupStore;
    private InMemoryServerStore serverStore;
    private BackupService backupService;
    private ServerCatalogService serverCatalogService;
    private Path tempMcDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        tempMcDir = tempDir.resolve("mc");
        Files.createDirectories(tempMcDir);
        Files.writeString(tempMcDir.resolve("server.properties"), "server-port=25565\n");
        Files.createDirectories(tempMcDir.resolve("world"));
        backupStore = new InMemoryBackupStore();
        serverStore = new InMemoryServerStore();
        ServerSupport serverSupport = new ServerSupport(serverStore, new ServerRuntimeState());
        serverCatalogService = new ServerCatalogService(serverSupport);
        backupService = new BackupService(backupStore, serverStore);
    }

    @Test
    void listBackups_withNoBackups_returnsEmptyList() {
        ManagedServer server = createServer();
        List<Backup> result = backupService.listBackups(server.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    void listBackups_withMultipleBackups_returnsAllBackups() {
        ManagedServer server = createServer();
        backupService.createBackup(server.getId());
        backupService.createBackup(server.getId());
        backupService.createBackup(server.getId());

        List<Backup> result = backupService.listBackups(server.getId());

        assertEquals(3, result.size());
    }

    @Test
    void listBackups_withNonExistingServer_returnsEmptyList() {
        List<Backup> result = backupService.listBackups(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void createBackup_withExistingServer_returnsBackup() {
        ManagedServer server = createServer();

        var result = backupService.createBackup(server.getId());

        assertTrue(result.isPresent());
        assertEquals(server.getId(), result.get().getServerId());
        assertNotNull(result.get().getFilename());
        assertNotNull(result.get().getFilePath());
        assertTrue(result.get().getFileSize() > 0);
        assertNotNull(result.get().getCreatedAt());
    }

    @Test
    void createBackup_withNonExistingServer_returnsEmpty() {
        var result = backupService.createBackup(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void createBackup_generatesUniqueFilenames() {
        ManagedServer server = createServer();

        var backup1 = backupService.createBackup(server.getId());
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        var backup2 = backupService.createBackup(server.getId());

        assertNotEquals(backup1.get().getFilename(), backup2.get().getFilename());
    }

    @Test
    void createBackup_filenameContainsServerId() {
        ManagedServer server = createServer();

        var backup = backupService.createBackup(server.getId());

        assertTrue(backup.get().getFilename().contains(String.valueOf(server.getId())));
    }

    @Test
    void createBackup_filepathContainsFilename() {
        ManagedServer server = createServer();

        var backup = backupService.createBackup(server.getId());

        assertTrue(backup.get().getFilePath().contains(backup.get().getFilename()));
    }

    @Test
    void listBackups_perServer_isolatesBackups() {
        ManagedServer server1 = createServer();
        ManagedServer server2 = createServer();

        backupService.createBackup(server1.getId());
        backupService.createBackup(server1.getId());
        backupService.createBackup(server2.getId());

        assertEquals(2, backupService.listBackups(server1.getId()).size());
        assertEquals(1, backupService.listBackups(server2.getId()).size());
    }

    @Test
    void createBackup_multipleBackups_storesAll() {
        ManagedServer server = createServer();

        for (int i = 0; i < 5; i++) {
            backupService.createBackup(server.getId());
        }

        List<Backup> backups = backupService.listBackups(server.getId());
        assertEquals(5, backups.size());
    }

    @Test
    void createBackup_assignsIdToBackup() {
        ManagedServer server = createServer();

        var backup = backupService.createBackup(server.getId());

        assertTrue(backup.get().getId() > 0);
    }

    private ManagedServer createServer() {
        ServerRequest request = new ServerRequest(
            "TestServer", "localhost", 25565, "password", 25575,
            "motd=Test", "/backups", tempMcDir.toString()
        );
        return serverCatalogService.createServer(request, 1L).orElseThrow();
    }
}
