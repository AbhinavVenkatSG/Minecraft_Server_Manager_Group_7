package app.server;

import domain.server.ManagedServer;

import java.util.Optional;

public class RestartServerService {
    private final ServerCatalogService catalogService;
    private final StartServerService startServerService;
    private final StopServerService stopServerService;

    public RestartServerService(
            ServerCatalogService catalogService,
            StartServerService startServerService,
            StopServerService stopServerService
    ) {
        this.catalogService = catalogService;
        this.startServerService = startServerService;
        this.stopServerService = stopServerService;
    }

    public Optional<ManagedServer> restartServer(long serverId) {
        Optional<ManagedServer> server = catalogService.getServer(serverId);
        if (server.isEmpty()) {
            return Optional.empty();
        }

        stopServerService.stopServer(serverId);
        return startServerService.startServer(serverId);
    }
}
