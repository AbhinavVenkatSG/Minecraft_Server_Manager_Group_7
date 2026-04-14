package infra.persistence;

import domain.server.ManagedServer;
import domain.server.ServerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServerStoreTest {

    @Test
    void saveAndReload_preservesServerData(@TempDir Path tempDir) {
        FileServerStore store = new FileServerStore(tempDir);
        LocalDateTime createdAt = LocalDateTime.now().withNano(0);
        LocalDateTime lastStarted = createdAt.plusMinutes(5);

        ManagedServer saved = store.save(new ManagedServer(
                0L,
                "Persisted Server",
                "localhost",
                25565,
                "secret",
                25575,
                ServerStatus.STOPPED,
                "motd=Persisted",
                "C:\\backups",
                "C:\\mc",
                7L,
                createdAt,
                lastStarted
        ));

        FileServerStore reloaded = new FileServerStore(tempDir);
        ManagedServer restored = reloaded.findById(saved.getId()).orElseThrow();

        assertEquals("Persisted Server", restored.getName());
        assertEquals(ServerStatus.STOPPED, restored.getStatus());
        assertEquals("motd=Persisted", restored.getServerProperties());
        assertEquals("C:\\mc", restored.getMinecraftDirectory());
        assertEquals(lastStarted, restored.getLastStarted());
    }

    @Test
    void reload_advancesGeneratedIds(@TempDir Path tempDir) {
        FileServerStore store = new FileServerStore(tempDir);
        store.save(new ManagedServer(
                0L,
                "First",
                "localhost",
                25565,
                "",
                25575,
                ServerStatus.STARTUP,
                "",
                "",
                "",
                1L,
                LocalDateTime.now().withNano(0),
                null
        ));

        FileServerStore reloaded = new FileServerStore(tempDir);
        ManagedServer second = reloaded.save(new ManagedServer(
                0L,
                "Second",
                "localhost",
                25566,
                "",
                25575,
                ServerStatus.STARTUP,
                "",
                "",
                "",
                1L,
                LocalDateTime.now().withNano(0),
                null
        ));

        assertEquals(2L, second.getId());
        assertEquals(2, reloaded.findAll().size());
        assertTrue(tempDir.resolve("servers").resolve("2.properties").toFile().exists());
    }
}
