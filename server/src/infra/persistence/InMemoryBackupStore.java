/**
 * @file InMemoryBackupStore.java
 * @brief In-memory implementation of BackupStore for testing.
 * @ingroup infra
 * @{
 */

package infra.persistence;

import app.backup.BackupStore;
import domain.backup.Backup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @class InMemoryBackupStore
 * @brief Non-persistent BackupStore implementation for unit testing.
 */
public class InMemoryBackupStore implements BackupStore {
    private final AtomicLong nextId = new AtomicLong(1);
    private final ConcurrentHashMap<Long, Backup> backups = new ConcurrentHashMap<>();

    @Override
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
    public java.util.Optional<Backup> findById(long backupId) {
        return java.util.Optional.ofNullable(backups.get(backupId));
    }

    @Override
    public Backup save(Backup backup) {
        if (backup.getId() == 0L) {
            backup.setId(nextId.getAndIncrement());
        }

        backups.put(backup.getId(), backup);
        return backup;
    }
}
