package domain.backup;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BackupTest {

    @Test
    void constructor_withAllFields_setsFieldsCorrectly() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 14, 30);
        Backup backup = new Backup(1L, 10L, "world.zip", "/backups/10/world.zip", 1048576L, createdAt);

        assertEquals(1L, backup.getId());
        assertEquals(10L, backup.getServerId());
        assertEquals("world.zip", backup.getFilename());
        assertEquals("/backups/10/world.zip", backup.getFilePath());
        assertEquals(1048576L, backup.getFileSize());
        assertEquals(createdAt, backup.getCreatedAt());
    }

    @Test
    void setId_updatesId() {
        Backup backup = createTestBackup();
        backup.setId(99L);
        assertEquals(99L, backup.getId());
    }

    @Test
    void getId_afterSetId_returnsUpdatedId() {
        Backup backup = new Backup(0L, 1L, "test.zip", "/path", 100L, LocalDateTime.now());
        assertEquals(0L, backup.getId());

        backup.setId(42L);
        assertEquals(42L, backup.getId());
    }

    @Test
    void immutableFields_remainUnchanged() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 5, 20, 8, 0);
        Backup backup = new Backup(1L, 5L, "backup.zip", "/path/to/backup.zip", 5000000L, createdAt);

        assertEquals(5L, backup.getServerId());
        assertEquals("backup.zip", backup.getFilename());
        assertEquals("/path/to/backup.zip", backup.getFilePath());
        assertEquals(5000000L, backup.getFileSize());
        assertEquals(createdAt, backup.getCreatedAt());
    }

    @Test
    void constructor_withZeroValues_worksCorrectly() {
        Backup backup = new Backup(0L, 0L, "", "", 0L, null);

        assertEquals(0L, backup.getId());
        assertEquals(0L, backup.getServerId());
        assertEquals("", backup.getFilename());
        assertEquals("", backup.getFilePath());
        assertEquals(0L, backup.getFileSize());
        assertNull(backup.getCreatedAt());
    }

    @Test
    void constructor_withLargeFileSize_worksCorrectly() {
        long largeSize = Long.MAX_VALUE;
        Backup backup = new Backup(1L, 1L, "large.zip", "/path", largeSize, LocalDateTime.now());

        assertEquals(largeSize, backup.getFileSize());
    }

    @Test
    void filename_withSpecialCharacters_worksCorrectly() {
        Backup backup = new Backup(
            1L, 1L, "backup_2024-03-15_14-30-00.zip",
            "/backups/server 1/backup_2024-03-15.zip",
            1024L, LocalDateTime.now()
        );

        assertEquals("backup_2024-03-15_14-30-00.zip", backup.getFilename());
        assertEquals("/backups/server 1/backup_2024-03-15.zip", backup.getFilePath());
    }

    private Backup createTestBackup() {
        return new Backup(
            1L, 10L, "world.zip", "/backups/10/world.zip",
            44040192L, LocalDateTime.of(2024, 3, 15, 14, 30)
        );
    }
}
