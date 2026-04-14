package test;

import domain.backup.Backup;
import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipFile;

public final class BackupServiceTests {
    private BackupServiceTests() {
    }

    public static void createsWorldBackupZipWhenServerIsStopped() throws Exception {
        try (TestEnvironment environment = new TestEnvironment()) {
            Path minecraftDirectory = environment.createMinecraftDirectory();
            ManagedServer server = environment.createServer(minecraftDirectory);

            Optional<Backup> backup = environment.backupService.createBackup(server.getId());
            Assertions.assertTrue(backup.isPresent(), "REQ-SVR-204: stopped servers should create backups.");
            Assertions.assertTrue(Files.exists(Path.of(backup.get().getFilePath())), "Backup archive should exist on disk.");
            Assertions.assertTrue(backup.get().getFileSize() > 0L, "Backup archive should have content.");

            try (ZipFile zipFile = new ZipFile(backup.get().getFilePath())) {
                Assertions.assertNotNull(zipFile.getEntry("level.dat"), "Backup should include files from the world directory.");
            }
        }
    }

    public static void rejectsBackupCreationWhileServerIsRunning() throws Exception {
        try (TestEnvironment environment = new TestEnvironment()) {
            Path minecraftDirectory = environment.createMinecraftDirectory();
            ManagedServer server = environment.createServer(minecraftDirectory);
            server.setStatus(ServerStatus.RUNNING);
            environment.getSupport().saveServer(server);

            Assertions.expectThrows(
                    IllegalStateException.class,
                    () -> environment.backupService.createBackup(server.getId()),
                    "REQ-SVR-204: running servers must not allow backups."
            );
        }
    }
}
