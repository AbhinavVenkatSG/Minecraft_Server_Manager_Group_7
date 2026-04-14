package app.server;

import domain.server.ManagedServer;
import domain.server.ServerStatus;

public class ServerShutdownService {
    private final ServerSupport support;
    private final StopServerService stopServerService;
    private final ServerTelemetryService telemetryService;

    public ServerShutdownService(
            ServerSupport support,
            StopServerService stopServerService,
            ServerTelemetryService telemetryService
    ) {
        this.support = support;
        this.stopServerService = stopServerService;
        this.telemetryService = telemetryService;
    }

    public void shutdown() {
        telemetryService.shutdown();

        for (ManagedServer server : support.listServers()) {
            ServerRuntimeState.RuntimeContext context = support.getRuntimeState().getRuntimeContexts().get(server.getId());
            if (context != null && server.getStatus() == ServerStatus.RUNNING) {
                try {
                    stopServerService.stopServer(server.getId());
                } catch (Exception ignored) {
                    context.getProcess().destroyForcibly();
                    support.finalizeStoppedServer(server.getId(), server);
                }
            }
        }
    }
}
