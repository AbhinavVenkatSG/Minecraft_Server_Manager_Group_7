package app.backup;

import domain.backup.Backup;

import java.util.List;

public interface BackupStore {
    List<Backup> findByServerId(long serverId);

    Backup save(Backup backup);
}
