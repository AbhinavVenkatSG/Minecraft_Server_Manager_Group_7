package app.backup;

import domain.backup.Backup;

import java.util.List;
import java.util.Optional;

public interface BackupStore {
    List<Backup> findByServerId(long serverId);

    Optional<Backup> findById(long backupId);

    Backup save(Backup backup);
}
