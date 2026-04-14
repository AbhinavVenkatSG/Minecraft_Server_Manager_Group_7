package infra.persistence;

import domain.backup.Backup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryBackupStoreTest {

    private InMemoryBackupStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryBackupStore();
    }

    @Test
    void save_newBackup_assignsIdAndReturnsBackup() {
        Backup backup = createBackup(0L, 10L, "world.zip");
        Backup saved = store.save(backup);

        assertEquals(1L, saved.getId());
        assertEquals("world.zip", saved.getFilename());
    }

    @Test
    void save_multipleBackups_assignsSequentialIds() {
        Backup backup1 = store.save(createBackup(0L, 10L, "backup1.zip"));
        Backup backup2 = store.save(createBackup(0L, 10L, "backup2.zip"));
        Backup backup3 = store.save(createBackup(0L, 10L, "backup3.zip"));

        assertEquals(1L, backup1.getId());
        assertEquals(2L, backup2.getId());
        assertEquals(3L, backup3.getId());
    }

    @Test
    void save_backupWithExistingId_doesNotChangeId() {
        Backup backup = store.save(createBackup(99L, 10L, "world.zip"));
        assertEquals(99L, backup.getId());
    }

    @Test
    void findByServerId_withExistingServerId_returnsBackups() {
        store.save(createBackup(0L, 10L, "backup1.zip"));
        store.save(createBackup(0L, 10L, "backup2.zip"));
        store.save(createBackup(0L, 20L, "backup3.zip"));

        List<Backup> server10Backups = store.findByServerId(10L);

        assertEquals(2, server10Backups.size());
        for (Backup backup : server10Backups) {
            assertEquals(10L, backup.getServerId());
        }
    }

    @Test
    void findByServerId_withNonExistingServerId_returnsEmptyList() {
        store.save(createBackup(0L, 10L, "backup.zip"));

        List<Backup> result = store.findByServerId(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByServerId_returnsBackupsSortedById() {
        store.save(createBackup(3L, 10L, "third.zip"));
        store.save(createBackup(1L, 10L, "first.zip"));
        store.save(createBackup(2L, 10L, "second.zip"));

        List<Backup> result = store.findByServerId(10L);

        assertEquals(3, result.size());
        assertEquals("first.zip", result.get(0).getFilename());
        assertEquals("second.zip", result.get(1).getFilename());
        assertEquals("third.zip", result.get(2).getFilename());
    }

    @Test
    void findByServerId_withNoBackups_returnsEmptyList() {
        List<Backup> result = store.findByServerId(10L);
        assertTrue(result.isEmpty());
    }

    @Test
    void save_multipleBackupsForSameServer_storesAll() {
        for (int i = 0; i < 5; i++) {
            store.save(createBackup(0L, 10L, "backup" + i + ".zip"));
        }

        List<Backup> result = store.findByServerId(10L);
        assertEquals(5, result.size());
    }

    @Test
    void save_backupsForDifferentServers_storesSeparately() {
        store.save(createBackup(0L, 10L, "server10.zip"));
        store.save(createBackup(0L, 20L, "server20.zip"));
        store.save(createBackup(0L, 30L, "server30.zip"));

        assertEquals(1, store.findByServerId(10L).size());
        assertEquals(1, store.findByServerId(20L).size());
        assertEquals(1, store.findByServerId(30L).size());
    }

    @Test
    void concurrentSaves_workCorrectly() throws InterruptedException {
        Thread[] threads = new Thread[10];
        Backup[] backups = new Backup[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                backups[index] = store.save(createBackup(0L, 10L, "backup" + index + ".zip"));
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }

        assertEquals(10, store.findByServerId(10L).size());
    }

    @Test
    void findByServerId_returnsLiveView() {
        List<Backup> view = store.findByServerId(10L);
        assertTrue(view.isEmpty());

        store.save(createBackup(0L, 10L, "new.zip"));
        assertEquals(1, store.findByServerId(10L).size());
    }

    @Test
    void save_updatesExistingBackup() {
        Backup backup = store.save(createBackup(0L, 10L, "original.zip"));

        Backup updated = createBackup(backup.getId(), 10L, "updated.zip");
        store.save(updated);

        List<Backup> result = store.findByServerId(10L);
        assertEquals(1, result.size());
        assertEquals("updated.zip", result.get(0).getFilename());
    }

    private Backup createBackup(long id, long serverId, String filename) {
        return new Backup(
            id, serverId, filename, "/backups/" + serverId + "/" + filename,
            44040192L, LocalDateTime.now()
        );
    }
}
