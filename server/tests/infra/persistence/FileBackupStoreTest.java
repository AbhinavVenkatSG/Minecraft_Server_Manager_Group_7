package infra.persistence;

import domain.backup.Backup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBackupStoreTest {

    @Test
    void saveAndReload_preservesBackupData(@TempDir Path tempDir) {
        FileBackupStore store = new FileBackupStore(tempDir);
        LocalDateTime createdAt = LocalDateTime.now().withNano(0);

        Backup saved = store.save(new Backup(
                0L,
                12L,
                "backup.zip",
                "C:\\mc\\backups\\backup.zip",
                1024L,
                createdAt
        ));

        FileBackupStore reloaded = new FileBackupStore(tempDir);
        Backup restored = reloaded.findById(saved.getId()).orElseThrow();

        assertEquals(12L, restored.getServerId());
        assertEquals("backup.zip", restored.getFilename());
        assertEquals(1024L, restored.getFileSize());
        assertEquals(createdAt, restored.getCreatedAt());
    }

    @Test
    void reload_advancesGeneratedIds(@TempDir Path tempDir) {
        FileBackupStore store = new FileBackupStore(tempDir);
        store.save(new Backup(
                0L,
                1L,
                "first.zip",
                "C:\\mc\\backups\\first.zip",
                100L,
                LocalDateTime.now().withNano(0)
        ));

        FileBackupStore reloaded = new FileBackupStore(tempDir);
        Backup second = reloaded.save(new Backup(
                0L,
                1L,
                "second.zip",
                "C:\\mc\\backups\\second.zip",
                200L,
                LocalDateTime.now().withNano(0)
        ));

        assertEquals(2L, second.getId());
        assertEquals(2, reloaded.findByServerId(1L).size());
        assertTrue(tempDir.resolve("backups").resolve("2.properties").toFile().exists());
    }
}
