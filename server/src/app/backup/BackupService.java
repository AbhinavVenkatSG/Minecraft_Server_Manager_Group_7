package app.backup;

import app.server.ServerStore;
import domain.backup.Backup;
import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupService {
    private final BackupStore backupStore;
    private final ServerStore serverStore;

    public BackupService(BackupStore backupStore, ServerStore serverStore) {
        this.backupStore = backupStore;
        this.serverStore = serverStore;
    }

    public List<Backup> listBackups(long serverId) {
        return backupStore.findByServerId(serverId);
    }

    public Optional<Backup> getBackup(long backupId) {
        return backupStore.findById(backupId);
    }

    public Optional<Path> resolveBackupFile(long backupId) {
        return backupStore.findById(backupId)
                .map(Backup::getFilePath)
                .map(Path::of)
                .filter(Files::exists);
    }

    public Optional<Backup> createBackup(long serverId) {
        Optional<ManagedServer> server = serverStore.findById(serverId);
        if (server.isEmpty()) {
            return Optional.empty();
        }

        ManagedServer managedServer = server.get();
        if (managedServer.getStatus() != ServerStatus.STOPPED) {
            throw new IllegalStateException("Server must be stopped before creating a backup.");
        }

        Path minecraftDirectory = Path.of(managedServer.getMinecraftDirectory());
        Path worldDirectory = minecraftDirectory.resolve("world");
        if (!Files.isDirectory(worldDirectory)) {
            throw new IllegalStateException("World directory not found at: " + worldDirectory);
        }

        try {
            Path backupDirectory = resolveBackupDirectory(managedServer, minecraftDirectory);
            Files.createDirectories(backupDirectory);

            String filename = "backup-" + serverId + "-" + System.currentTimeMillis() + ".zip";
            Path backupFile = backupDirectory.resolve(filename);
            zipDirectory(worldDirectory, backupFile);

            Backup backup = new Backup(
                    0L,
                    serverId,
                    filename,
                    backupFile.toAbsolutePath().toString(),
                    Files.size(backupFile),
                    LocalDateTime.now()
            );

            return Optional.of(backupStore.save(backup));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create backup.", exception);
        }
    }

    private Path resolveBackupDirectory(ManagedServer server, Path minecraftDirectory) {
        if (server.getBackupPath() != null && !server.getBackupPath().isBlank()) {
            return Path.of(server.getBackupPath());
        }
        return minecraftDirectory.resolve("backups");
    }

    private void zipDirectory(Path sourceDirectory, Path targetZip) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(
                targetZip,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        ); ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            Files.walk(sourceDirectory)
                    .filter(Files::isRegularFile)
                    .forEach(path -> writeZipEntry(sourceDirectory, path, zipOutputStream));
        }
    }

    private void writeZipEntry(Path sourceDirectory, Path path, ZipOutputStream zipOutputStream) {
        String entryName = sourceDirectory.relativize(path).toString().replace('\\', '/');
        try (InputStream inputStream = Files.newInputStream(path)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            inputStream.transferTo(zipOutputStream);
            zipOutputStream.closeEntry();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to zip file: " + path, exception);
        }
    }
}
