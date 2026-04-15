/**
 * @file BackupStore.java
 * @brief Repository interface for backup persistence operations.
 * @{
 */

package app.backup;

import domain.backup.Backup;

import java.util.List;
import java.util.Optional;

/**
 * @interface BackupStore
 * @brief Abstract interface for backup metadata persistence.
 */
public interface BackupStore {
    /**
     * @brief Retrieves all backups for a specific server.
     * @param serverId The server ID
     * @return List of backups ordered by ID
     */
    List<Backup> findByServerId(long serverId);

    /**
     * @brief Finds a backup by its ID.
     * @param backupId The backup ID
     * @return The backup if found
     */
    Optional<Backup> findById(long backupId);

    /**
     * @brief Persists a backup record.
     * @param backup The backup to save
     * @return The saved backup with assigned ID
     */
    Backup save(Backup backup);
}
