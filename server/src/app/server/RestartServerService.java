package app.server;

import domain.server.ManagedServer;

import java.util.Optional;

/**
 * Restarts a managed server by delegating to the stop and start services.
 */
public class RestartServerService {
    private final ServerCatalogService catalogService;
    private final StartServerService startServerService;
    private final StopServerService stopServerService;

    /**
     * Creates a restart service from the existing server operations.
     *
     * @param catalogService catalog lookup service
     * @param startServerService start operation
     * @param stopServerService stop operation
     */
    public RestartServerService(
            ServerCatalogService catalogService,
            StartServerService startServerService,
            StopServerService stopServerService
    ) {
        this.catalogService = catalogService;
        this.startServerService = startServerService;
        this.stopServerService = stopServerService;
    }

    /**
     * Restarts the given server when it exists.
     *
     * @param serverId managed server id
     * @return the restarted server when it exists
     */
    public Optional<ManagedServer> restartServer(long serverId) {
        Optional<ManagedServer> server = catalogService.getServer(serverId);
        if (server.isEmpty()) {
            return Optional.empty();
        }

        stopServerService.stopServer(serverId);
        return startServerService.startServer(serverId);
    }
}
