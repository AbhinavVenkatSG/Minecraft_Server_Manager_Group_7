package app.server;

import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.io.IOException;
import java.util.List;

/**
 * Sends commands to the running server process and exposes recent console output.
 */
public class ServerConsoleService {
    private final ServerSupport support;

    /**
     * Creates a console service backed by shared server helpers.
     *
     * @param support shared server helpers and runtime state
     */
    public ServerConsoleService(ServerSupport support) {
        this.support = support;
    }

    /**
     * Writes a command to the Minecraft process standard input.
     *
     * @param serverId managed server id
     * @param command command text to send
     * @return a short confirmation message
     */
    public String sendConsoleCommand(long serverId, String command) {
        if (support.isBlank(command)) {
            throw new IllegalArgumentException("Console command cannot be empty.");
        }

        ManagedServer server = support.requireServer(serverId);
        ServerRuntimeState.RuntimeContext context = support.getRuntimeState().getRuntimeContexts().get(serverId);
        if (server.getStatus() != ServerStatus.RUNNING || context == null) {
            throw new IllegalStateException("Server is not running.");
        }

        try {
            context.getInput().write(command.trim());
            context.getInput().newLine();
            context.getInput().flush();
            return "Console command sent: " + command.trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to send console command.", exception);
        }
    }

    /**
     * Returns the in-memory console buffer for a server.
     *
     * @param serverId managed server id
     * @return recent console lines, or an empty list when no process is tracked
     */
    public List<String> getRecentConsoleLines(long serverId) {
        ServerRuntimeState.RuntimeContext context = support.getRuntimeState().getRuntimeContexts().get(serverId);
        if (context == null) {
            return List.of();
        }

        synchronized (context.getConsoleLines()) {
            return List.copyOf(context.getConsoleLines());
        }
    }
}
