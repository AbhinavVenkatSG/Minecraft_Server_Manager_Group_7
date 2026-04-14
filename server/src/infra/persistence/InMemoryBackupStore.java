package infra.persistence;

import app.backup.BackupStore;
import domain.backup.Backup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    public Backup save(Backup backup) {
        if (backup.getId() == 0L) {
            backup.setId(nextId.getAndIncrement());
        }

        backups.put(backup.getId(), backup);
        return backup;
    }
}
