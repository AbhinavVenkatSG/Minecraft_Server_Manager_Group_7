package app.server;

import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Launches the configured Minecraft process and tracks its runtime context.
 */
public class StartServerService {
    private final ServerSupport support;

    /**
     * Creates a start service backed by shared server helpers.
     *
     * @param support shared server helpers and runtime state
     */
    public StartServerService(ServerSupport support) {
        this.support = support;
    }

    /**
     * Starts the server process for the requested managed server.
     *
     * @param serverId managed server id
     * @return the updated server when it exists
     */
    public Optional<ManagedServer> startServer(long serverId) {
        Optional<ManagedServer> server = support.getServer(serverId);
        if (server.isEmpty()) {
            return Optional.empty();
        }

        ManagedServer managedServer = server.get();
        if (managedServer.getStatus() == ServerStatus.RUNNING || support.getRuntimeState().getRuntimeContexts().containsKey(serverId)) {
            throw new IllegalStateException("Server is already running.");
        }

        Path minecraftDirectory = support.requireMinecraftDirectory(managedServer);
        Path startScript = support.resolveStartScript(minecraftDirectory);

        try {
            managedServer.setStatus(ServerStatus.BLOCKED);
            support.saveServer(managedServer);

            // The local setup uses batch files, so the process must be launched through cmd.
            Process process = new ProcessBuilder("cmd.exe", "/c", startScript.getFileName().toString())
                    .directory(minecraftDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();

            ServerRuntimeState.RuntimeContext context = new ServerRuntimeState.RuntimeContext(process);
            support.getRuntimeState().getRuntimeContexts().put(serverId, context);
            support.startOutputPump(serverId, managedServer, context);

            managedServer.setStatus(ServerStatus.RUNNING);
            managedServer.setLastStarted(LocalDateTime.now());
            support.saveServer(managedServer);
            return Optional.of(managedServer);
        } catch (IOException exception) {
            support.getRuntimeState().getRuntimeContexts().remove(serverId);
            managedServer.setStatus(ServerStatus.STOPPED);
            support.saveServer(managedServer);
            throw new IllegalStateException("Unable to start Minecraft server.", exception);
        }
    }
}
