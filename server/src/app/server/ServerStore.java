package app.server;

import domain.server.ManagedServer;

import java.util.List;
import java.util.Optional;

public interface ServerStore {
    List<ManagedServer> findAll();

    Optional<ManagedServer> findById(long id);

    ManagedServer save(ManagedServer server);
}
