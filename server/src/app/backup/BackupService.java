package app.backup;

import app.server.ServerStore;
import domain.backup.Backup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    public Optional<Backup> createBackup(long serverId) {
        if (serverStore.findById(serverId).isEmpty()) {
            return Optional.empty();
        }

        String filename = "backup-" + serverId + "-" + System.currentTimeMillis() + ".zip";
        Backup backup = new Backup(
                0L,
                serverId,
                filename,
                "backups/" + filename,
                1024L * 1024L * 42L,
                LocalDateTime.now()
        );

        return Optional.of(backupStore.save(backup));
    }
}
