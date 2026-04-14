package app.server;

import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StopServerService {
    private final ServerSupport support;

    public StopServerService(ServerSupport support) {
        this.support = support;
    }

    public Optional<ManagedServer> stopServer(long serverId) {
        Optional<ManagedServer> server = support.getServer(serverId);
        if (server.isEmpty()) {
            return Optional.empty();
        }

        ManagedServer managedServer = server.get();
        ServerRuntimeState.RuntimeContext context = support.getRuntimeState().getRuntimeContexts().get(serverId);
        if (managedServer.getStatus() != ServerStatus.RUNNING || context == null) {
            throw new IllegalStateException("Server is not running.");
        }

        managedServer.setStatus(ServerStatus.BLOCKED);
        support.saveServer(managedServer);

        try {
            context.getInput().write("stop");
            context.getInput().newLine();
            context.getInput().flush();
            boolean exited = context.getProcess().waitFor(30, TimeUnit.SECONDS);
            if (!exited) {
                context.getProcess().destroy();
                exited = context.getProcess().waitFor(10, TimeUnit.SECONDS);
            }
            if (!exited) {
                context.getProcess().destroyForcibly();
                context.getProcess().waitFor(5, TimeUnit.SECONDS);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to stop Minecraft server cleanly.", exception);
        } finally {
            support.finalizeStoppedServer(serverId, managedServer);
        }

        return Optional.of(managedServer);
    }
}
