package app.server;

import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Stops a running Minecraft process, escalating from graceful shutdown to kill when needed.
 */
public class StopServerService {
    private final ServerSupport support;

    /**
     * Creates a stop service backed by shared server helpers.
     *
     * @param support shared server helpers and runtime state
     */
    public StopServerService(ServerSupport support) {
        this.support = support;
    }

    /**
     * Stops the requested server and persists the final stopped state.
     *
     * @param serverId managed server id
     * @return the updated server when it exists
     */
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
