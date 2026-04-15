package infra.persistence;

import app.backup.BackupStore;
import domain.backup.Backup;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * File-backed {@link BackupStore} implementation for backup metadata.
 */
public class FileBackupStore implements BackupStore {
    private final Path backupDirectory;
    private final AtomicLong nextId = new AtomicLong(1L);
    private final ConcurrentHashMap<Long, Backup> backups = new ConcurrentHashMap<>();

    /**
     * Creates the store, ensures its directories exist, and loads persisted backups.
     *
     * @param rootDirectory application data directory
     */
    public FileBackupStore(Path rootDirectory) {
        try {
            this.backupDirectory = rootDirectory.resolve("backups");
            Files.createDirectories(backupDirectory);
            loadBackups();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize backup store.", exception);
        }
    }

    @Override
    /**
     * Returns all backups for the given server ordered by id.
     *
     * @param serverId managed server id
     * @return matching backups
     */
    public List<Backup> findByServerId(long serverId) {
        List<Backup> matches = new ArrayList<>();
        for (Backup backup : backups.values()) {
            if (backup.getServerId() == serverId) {
                matches.add(backup);
            }
        }
        matches.sort(Comparator.comparingLong(Backup::getId));
        return matches;
    }

    @Override
    /**
     * Finds a backup by id.
     *
     * @param backupId backup id
     * @return matching backup when found
     */
    public Optional<Backup> findById(long backupId) {
        return Optional.ofNullable(backups.get(backupId));
    }

    @Override
    /**
     * Saves a backup, assigning the next numeric id when needed.
     *
     * @param backup backup to persist
     * @return the saved backup
     */
    public synchronized Backup save(Backup backup) {
        if (backup.getId() == 0L) {
            backup.setId(nextId.getAndIncrement());
        } else {
            nextId.set(Math.max(nextId.get(), backup.getId() + 1));
        }

        backups.put(backup.getId(), backup);
        writeBackup(backup);
        return backup;
    }

    private void loadBackups() throws Exception {
        try (var paths = Files.list(backupDirectory)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .forEach(this::readBackupUnchecked);
        }
    }

    private void readBackupUnchecked(Path path) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(Files.readString(path)));

            Backup backup = new Backup(
                    Long.parseLong(properties.getProperty("id", "0")),
                    Long.parseLong(properties.getProperty("serverId", "0")),
                    properties.getProperty("filename", ""),
                    properties.getProperty("filePath", ""),
                    Long.parseLong(properties.getProperty("fileSize", "0")),
                    LocalDateTime.parse(properties.getProperty("createdAt"))
            );

            backups.put(backup.getId(), backup);
            nextId.set(Math.max(nextId.get(), backup.getId() + 1));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load backup file: " + path, exception);
        }
    }

    private void writeBackup(Backup backup) {
        try {
            Properties properties = new Properties();
            properties.setProperty("id", Long.toString(backup.getId()));
            properties.setProperty("serverId", Long.toString(backup.getServerId()));
            properties.setProperty("filename", backup.getFilename());
            properties.setProperty("filePath", backup.getFilePath());
            properties.setProperty("fileSize", Long.toString(backup.getFileSize()));
            properties.setProperty("createdAt", backup.getCreatedAt().toString());

            StringWriter writer = new StringWriter();
            properties.store(writer, null);
            Files.writeString(backupDirectory.resolve(backup.getId() + ".properties"), writer.toString());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist backup.", exception);
        }
    }
}
