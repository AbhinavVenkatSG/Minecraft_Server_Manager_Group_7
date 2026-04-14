package app.server;

import java.io.Console;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Scanner;

import app.server.dto.ServerRequest;
import domain.server.ManagedServer;
import domain.server.ServerStatus;

public class ServerCatalogService {
    private final ServerSupport support;

    public ServerCatalogService(ServerSupport support) {
        this.support = support;
        this.support.normalizePersistedState();
    }

    public Optional<ManagedServer> getServer(long serverId) {
        return support.getServer(serverId);
    }

    public Optional<ManagedServer> createServer(ServerRequest request, long ownerId) {
        if (request == null || support.isBlank(request.getName()) || support.isBlank(request.getHost()) || request.getPort() <= 0) {
            return Optional.empty();
        }
        if (!support.listServers().isEmpty()) {
            return Optional.empty();
        }

        String minecraftDirectory = support.safe(request.getMinecraftDirectory());
        if (!minecraftDirectory.isBlank()) {
            support.validateMinecraftDirectory(Path.of(minecraftDirectory));
        }

        ManagedServer server = new ManagedServer(
                0L,
                request.getName().trim(),
                request.getHost().trim(),
                request.getPort(),
                support.safe(request.getRconPassword()),
                request.getRconPort() > 0 ? request.getRconPort() : 25575,
                minecraftDirectory.isBlank() ? ServerStatus.STARTUP : ServerStatus.STOPPED,
                support.defaultServerProperties(minecraftDirectory, request.getServerProperties()),
                support.safe(request.getBackupPath()),
                minecraftDirectory,
                ownerId,
                LocalDateTime.now(),
                null
        );

        return Optional.of(support.saveServer(server));
    }

    public void ensureInteractiveSetup() {
        if (!support.listServers().isEmpty()) {
            return;
        }

        Console console = System.console();
        Scanner scanner = console == null ? new Scanner(System.in) : null;

        while (true) {
            String rawDirectory = console != null
                    ? console.readLine("Enter the Minecraft server directory: ")
                    : promptAndRead(scanner, "Enter the Minecraft server directory: ");

            if (support.isBlank(rawDirectory)) {
                continue;
            }

            try {
                Path directory = support.validateMinecraftDirectory(Path.of(rawDirectory.trim()));
                String serverName = directory.getFileName() == null ? "Local Minecraft Server" : directory.getFileName().toString();
                createServer(new ServerRequest(
                        serverName,
                        "localhost",
                        25565,
                        "",
                        25575,
                        support.readOptionalFile(directory.resolve("server.properties")),
                        "",
                        directory.toAbsolutePath().toString()
                ), 1L);
                System.out.println("Minecraft server directory configured: " + directory.toAbsolutePath());
                return;
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }

    public String readMinecraftDirectory(long serverId) {
        return support.requireServer(serverId).getMinecraftDirectory();
    }

    public ManagedServer updateMinecraftDirectory(long serverId, String rawDirectory) {
        ManagedServer server = support.requireServer(serverId);
        if (server.getStatus() == ServerStatus.RUNNING) {
            throw new IllegalStateException("Stop the server before changing the Minecraft directory.");
        }

        Path directory = support.validateMinecraftDirectory(Path.of(rawDirectory.trim()));
        server.setMinecraftDirectory(directory.toAbsolutePath().toString());
        server.setServerProperties(support.readOptionalFile(directory.resolve("server.properties")));
        server.setStatus(ServerStatus.STOPPED);
        return support.saveServer(server);
    }

    private String promptAndRead(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner != null && scanner.hasNextLine() ? scanner.nextLine() : "";
    }
}
