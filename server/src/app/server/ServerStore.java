/**
 * @file ServerStore.java
 * @brief Repository interface for managed server persistence operations.
 * @{
 */

package app.server;

import domain.server.ManagedServer;

import java.util.List;
import java.util.Optional;

/**
 * @interface ServerStore
 * @brief Abstract interface for managed server metadata persistence.
 */
public interface ServerStore {
    /**
     * @brief Retrieves all managed servers.
     * @return List of all servers
     */
    List<ManagedServer> findAll();

    /**
     * @brief Finds a server by ID.
     * @param id The server ID
     * @return The server if found
     */
    Optional<ManagedServer> findById(long id);

    /**
     * @brief Persists a server configuration.
     * @param server The server to save
     * @return The saved server with assigned ID
     */
    ManagedServer save(ManagedServer server);
}
