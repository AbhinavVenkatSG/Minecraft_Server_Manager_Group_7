/**
 * @file InMemoryServerStore.java
 * @brief In-memory implementation of ServerStore for testing.
 * @ingroup infra
 * @{
 */

package infra.persistence;

import app.server.ServerStore;
import domain.server.ManagedServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @class InMemoryServerStore
 * @brief Non-persistent ServerStore implementation for unit testing.
 */
public class InMemoryServerStore implements ServerStore {
    private final AtomicLong nextId = new AtomicLong(1);
    private final ConcurrentHashMap<Long, ManagedServer> servers = new ConcurrentHashMap<>();

    @Override
    public List<ManagedServer> findAll() {
        List<ManagedServer> values = new ArrayList<>(servers.values());
        values.sort(Comparator.comparingLong(ManagedServer::getId));
        return values;
    }

    @Override
    public Optional<ManagedServer> findById(long id) {
        return Optional.ofNullable(servers.get(id));
    }

    @Override
    public ManagedServer save(ManagedServer server) {
        if (server.getId() == 0L) {
            server.setId(nextId.getAndIncrement());
        }

        servers.put(server.getId(), server);
        return server;
    }
}
