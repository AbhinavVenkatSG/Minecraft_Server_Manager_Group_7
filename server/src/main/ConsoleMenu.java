package main;

import app.auth.AuthService;
import app.auth.dto.RegisterRequest;
import app.backup.BackupService;
import app.server.RestartServerService;
import app.server.ServerCatalogService;
import app.server.ServerConsoleService;
import app.server.ServerFileService;
import app.server.ServerTelemetryService;
import app.server.StartServerService;
import app.server.StopServerService;
import domain.backup.Backup;
import domain.server.ManagedServer;
import domain.server.TelemetrySnapshot;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ConsoleMenu {
    private static final double BYTES_PER_GIGABYTE = 1024d * 1024d * 1024d;

    private final AuthService authService;
    private final ServerCatalogService serverCatalogService;
    private final StartServerService startServerService;
    private final StopServerService stopServerService;
    private final RestartServerService restartServerService;
    private final ServerConsoleService serverConsoleService;
    private final ServerFileService serverFileService;
    private final ServerTelemetryService serverTelemetryService;
    private final BackupService backupService;
    private final Scanner scanner;

    public ConsoleMenu(
            AuthService authService,
            ServerCatalogService serverCatalogService,
            StartServerService startServerService,
            StopServerService stopServerService,
            RestartServerService restartServerService,
            ServerConsoleService serverConsoleService,
            ServerFileService serverFileService,
            ServerTelemetryService serverTelemetryService,
            BackupService backupService
    ) {
        this.authService = authService;
        this.serverCatalogService = serverCatalogService;
        this.startServerService = startServerService;
        this.stopServerService = stopServerService;
        this.restartServerService = restartServerService;
        this.serverConsoleService = serverConsoleService;
        this.serverFileService = serverFileService;
        this.serverTelemetryService = serverTelemetryService;
        this.backupService = backupService;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        boolean running = true;
        while (running) {
            ManagedServer server = requirePrimaryServer();
            printMenu(server);
            String choice = readLine("Select an option: ");

            try {
                switch (choice) {
                    case "1" -> startServer(server);
                    case "2" -> stopServer(server);
                    case "3" -> restartServer(server);
                    case "4" -> showPlayerCount(server);
                    case "5" -> showRamUsage(server);
                    case "6" -> showCpuUsage(server);
                    case "7" -> readWhitelist(server);
                    case "8" -> writeWhitelist(server);
                    case "9" -> readServerProperties(server);
                    case "10" -> writeServerProperties(server);
                    case "11" -> readStartParameters(server);
                    case "12" -> writeStartParameters(server);
                    case "13" -> createBackup(server);
                    case "14" -> showLatestConsoleLog(server);
                    case "15" -> changeMinecraftDirectory(server);
                    case "16" -> showTelemetryLog(server);
                    case "17" -> showServerSize(server);
                    case "18" -> addUser();
                    case "19" -> running = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception exception) {
                System.out.println("Operation failed: " + exception.getMessage());
            }

            if (running) {
                System.out.println();
            }
        }
    }

    private void printMenu(ManagedServer server) {
        System.out.println();
        System.out.println("=== Minecraft Server Manager CLI ===");
        System.out.println("Server: " + server.getName());
        System.out.println("State: " + server.getStatus());
        System.out.println("Directory: " + blankFallback(server.getMinecraftDirectory()));
        System.out.println();
        System.out.println("1. Start Server");
        System.out.println("2. Stop Server");
        System.out.println("3. Restart Server");
        System.out.println("4. View player count");
        System.out.println("5. View RAM usage");
        System.out.println("6. Show CPU usage");
        System.out.println("7. Read Whitelist");
        System.out.println("8. Write Whitelist");
        System.out.println("9. Read Server.properties");
        System.out.println("10. Write Server.properties");
        System.out.println("11. Read Start Parameters");
        System.out.println("12. Write Start Parameters");
        System.out.println("13. Create server backup");
        System.out.println("14. Show latest console log");
        System.out.println("15. Change mc directory");
        System.out.println("16. View telemetry log");
        System.out.println("17. View server size (in gb)");
        System.out.println("18. Add user");
        System.out.println("19. Exit");
        System.out.println();
    }

    private void startServer(ManagedServer server) {
        startServerService.startServer(server.getId());
        System.out.println("Server started.");
    }

    private void stopServer(ManagedServer server) {
        stopServerService.stopServer(server.getId());
        System.out.println("Server stopped.");
    }

    private void restartServer(ManagedServer server) {
        restartServerService.restartServer(server.getId());
        System.out.println("Server restarted.");
    }

    private void showPlayerCount(ManagedServer server) {
        TelemetrySnapshot telemetry = serverTelemetryService.getTelemetry(server.getId());
        System.out.println("Player count: " + telemetry.getPlayerCount());
        System.out.println("Players: " + joinOrNone(telemetry.getOnlinePlayers()));
    }

    private void showRamUsage(ManagedServer server) {
        TelemetrySnapshot telemetry = serverTelemetryService.getTelemetry(server.getId());
        System.out.println("Minecraft RAM usage: " + formatBytes(telemetry.getMinecraftProcessMemoryBytes()));
        System.out.println("System RAM usage: "
                + formatBytes(telemetry.getSystemMemoryUsedBytes())
                + " / "
                + formatBytes(telemetry.getSystemMemoryTotalBytes()));
        System.out.println("JVM initial allocation: " + blankFallback(telemetry.getJvmInitialRam()));
        System.out.println("JVM max allocation: " + blankFallback(telemetry.getJvmAllocatedRam()));
    }

    private void showCpuUsage(ManagedServer server) {
        TelemetrySnapshot telemetry = serverTelemetryService.getTelemetry(server.getId());
        System.out.println("Minecraft CPU usage: " + formatCpuLoad(telemetry.getMinecraftProcessCpuLoadPercent()));
        System.out.println("System CPU usage: " + formatCpuLoad(telemetry.getSystemCpuLoadPercent()));
    }

    private void readWhitelist(ManagedServer server) {
        System.out.println("--- whitelist ---");
        System.out.println(serverFileService.readWhitelist(server.getId()));
    }

    private void writeWhitelist(ManagedServer server) {
        String content = readMultiline("Enter whitelist content. Type END on its own line to finish.");
        serverFileService.writeWhitelist(server.getId(), content);
        System.out.println("Whitelist updated.");
    }

    private void readServerProperties(ManagedServer server) {
        System.out.println("--- server.properties ---");
        System.out.println(serverFileService.readServerProperties(server.getId()));
    }

    private void writeServerProperties(ManagedServer server) {
        String content = readMultiline("Enter server.properties content. Type END on its own line to finish.");
        serverFileService.writeServerProperties(server.getId(), content);
        System.out.println("server.properties updated.");
    }

    private void readStartParameters(ManagedServer server) {
        System.out.println("Start parameters:");
        System.out.println(serverFileService.readStartParameters(server.getId()));
    }

    private void writeStartParameters(ManagedServer server) {
        String content = readLine("New start parameter string: ");
        serverFileService.writeStartParameters(server.getId(), content);
        System.out.println("Start parameters updated.");
    }

    private void createBackup(ManagedServer server) {
        Optional<Backup> backup = backupService.createBackup(server.getId());
        if (backup.isEmpty()) {
            System.out.println("Backup could not be created.");
            return;
        }

        Backup created = backup.get();
        System.out.println("Backup created:");
        System.out.println("File: " + created.getFilename());
        System.out.println("Path: " + created.getFilePath());
        System.out.println("Size: " + formatBytes(created.getFileSize()));
    }

    private void showLatestConsoleLog(ManagedServer server) {
        List<String> lines = serverConsoleService.getRecentConsoleLines(server.getId());
        if (lines.isEmpty()) {
            System.out.println("No console output available.");
            return;
        }

        System.out.println("--- Latest console log ---");
        int startIndex = Math.max(0, lines.size() - 20);
        for (int index = startIndex; index < lines.size(); index++) {
            System.out.println(lines.get(index));
        }
    }

    private void changeMinecraftDirectory(ManagedServer server) {
        String path = readLine("New Minecraft directory path: ");
        ManagedServer updated = serverCatalogService.updateMinecraftDirectory(server.getId(), path);
        System.out.println("Minecraft directory updated to: " + updated.getMinecraftDirectory());
    }

    private void showTelemetryLog(ManagedServer server) {
        serverTelemetryService.getTelemetry(server.getId());
        List<String> history = serverTelemetryService.getTelemetryHistory(server.getId());
        if (history.isEmpty()) {
            System.out.println("No telemetry history available.");
            return;
        }

        System.out.println("--- Telemetry log ---");
        for (String entry : history) {
            System.out.println(entry);
        }
    }

    private void showServerSize(ManagedServer server) {
        TelemetrySnapshot telemetry = serverTelemetryService.getTelemetry(server.getId());
        System.out.printf("Server size: %.2f GB%n", telemetry.getMinecraftDirectorySizeBytes() / BYTES_PER_GIGABYTE);
    }

    private void addUser() {
        String username = readLine("New username: ");
        if (username.isBlank()) {
            System.out.println("Username cannot be blank.");
            return;
        }
        String password = readLine("New password: ");
        if (password.isBlank()) {
            System.out.println("Password cannot be blank.");
            return;
        }
        boolean created = authService.register(new RegisterRequest(username.trim(), password)).isPresent();
        if (created) {
            System.out.println("User '" + username.trim() + "' created.");
        } else {
            System.out.println("Failed to create user. Username may already exist.");
        }
    }

    private ManagedServer requirePrimaryServer() {
        return serverCatalogService.getServer(1L)
                .orElseThrow(() -> new IllegalStateException("No server configured."));
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }

    private String readMultiline(String prompt) {
        System.out.println(prompt);
        StringBuilder builder = new StringBuilder();

        while (true) {
            String line = readLine("");
            if ("END".equals(line)) {
                break;
            }
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(line);
        }

        return builder.toString();
    }

    private String joinOrNone(List<String> values) {
        return values.isEmpty() ? "(none)" : String.join(", ", values);
    }

    private String blankFallback(String value) {
        return value == null || value.isBlank() ? "(not set)" : value;
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0L) {
            return "0 B";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (value >= 1024d && unitIndex < units.length - 1) {
            value /= 1024d;
            unitIndex++;
        }
        return String.format("%.2f %s", value, units[unitIndex]);
    }

    private String formatCpuLoad(Double cpuLoadPercent) {
        return cpuLoadPercent == null ? "Failed to read" : String.format("%.2f%%", cpuLoadPercent);
    }
}
