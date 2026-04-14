package app.server;

import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.io.IOException;
import java.util.List;

public class ServerConsoleService {
    private final ServerSupport support;

    public ServerConsoleService(ServerSupport support) {
        this.support = support;
    }

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
