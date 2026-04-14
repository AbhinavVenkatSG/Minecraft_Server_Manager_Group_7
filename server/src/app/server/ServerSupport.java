package app.server;

import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerSupport {
    private static final int MAX_CONSOLE_LINES = 250;
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile(": ([A-Za-z0-9_]{1,16}) joined the game");
    private static final Pattern PLAYER_LEAVE_PATTERN = Pattern.compile(": ([A-Za-z0-9_]{1,16}) left the game");

    private final ServerStore serverStore;
    private final ServerRuntimeState runtimeState;

    public ServerSupport(ServerStore serverStore, ServerRuntimeState runtimeState) {
        this.serverStore = serverStore;
        this.runtimeState = runtimeState;
    }

    public ServerStore getServerStore() {
        return serverStore;
    }

    public ServerRuntimeState getRuntimeState() {
        return runtimeState;
    }

    public List<ManagedServer> listServers() {
        return serverStore.findAll();
    }

    public Optional<ManagedServer> getServer(long serverId) {
        return serverStore.findById(serverId);
    }

    public ManagedServer saveServer(ManagedServer server) {
        return serverStore.save(server);
    }

    public ManagedServer requireServer(long serverId) {
        return serverStore.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found."));
    }

    public void normalizePersistedState() {
        for (ManagedServer server : serverStore.findAll()) {
            if (server.getStatus() == ServerStatus.RUNNING || server.getStatus() == ServerStatus.BLOCKED) {
                server.setStatus(isBlank(server.getMinecraftDirectory()) ? ServerStatus.STARTUP : ServerStatus.STOPPED);
                serverStore.save(server);
            }
        }
    }

    public Path validateMinecraftDirectory(Path directory) {
        Path normalized = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException("Minecraft directory does not exist: " + normalized);
        }

        boolean looksValid = Files.exists(normalized.resolve("server.properties"))
                || Files.exists(normalized.resolve("start.bat"))
                || Files.exists(normalized.resolve("start.cmd"))
                || Files.exists(normalized.resolve("world"));
        if (!looksValid) {
            throw new IllegalArgumentException("Directory does not look like a Minecraft server folder: " + normalized);
        }

        return normalized;
    }

    public Path requireMinecraftDirectory(ManagedServer server) {
        if (isBlank(server.getMinecraftDirectory())) {
            throw new IllegalStateException("Minecraft directory has not been configured.");
        }
        return validateMinecraftDirectory(Path.of(server.getMinecraftDirectory()));
    }

    public Path resolveStartScript(Path minecraftDirectory) {
        Path[] candidates = new Path[] {
                minecraftDirectory.resolve("start.bat"),
                minecraftDirectory.resolve("start.cmd"),
                minecraftDirectory.resolve("run.bat")
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("No start script found in the Minecraft directory.");
    }

    public Path resolveWhitelistFile(Path minecraftDirectory) {
        Path json = minecraftDirectory.resolve("whitelist.json");
        if (Files.exists(json)) {
            return json;
        }

        Path text = minecraftDirectory.resolve("whitelist.txt");
        if (Files.exists(text)) {
            return text;
        }

        return json;
    }

    public void ensureServerStopped(ManagedServer server) {
        if (server.getStatus() == ServerStatus.RUNNING || server.getStatus() == ServerStatus.BLOCKED) {
            throw new IllegalStateException("Stop the server before editing files.");
        }
    }

    public String readOptionalFile(Path path) {
        return Files.exists(path) ? readRequiredFile(path) : "";
    }

    public String readRequiredFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read file: " + path, exception);
        }
    }

    public void writeTextFile(Path path, String content) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write file: " + path, exception);
        }
    }

    public long computeDirectorySize(Path root) {
        try {
            return Files.walk(root)
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException exception) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to measure Minecraft directory size.", exception);
        }
    }

    public long[] getDriveMetrics(Path path) {
        try {
            FileStore fileStore = Files.getFileStore(path);
            return new long[] {fileStore.getTotalSpace(), fileStore.getUsableSpace()};
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to measure available drive space.", exception);
        }
    }

    public List<String> readNewLogLines(long serverId, Path latestLogPath) {
        if (!Files.exists(latestLogPath)) {
            return List.of();
        }

        try {
            long fileSize = Files.size(latestLogPath);
            long offset = runtimeState.getLatestLogOffsets().getOrDefault(serverId, 0L);
            if (offset > fileSize) {
                offset = 0L;
            }

            List<String> allLines = Files.readAllLines(latestLogPath, StandardCharsets.UTF_8);
            int startIndex = estimateLineIndex(allLines, offset);
            List<String> newLines = allLines.subList(startIndex, allLines.size());
            runtimeState.getLatestLogOffsets().put(serverId, fileSize);
            return List.copyOf(newLines);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read latest.log.", exception);
        }
    }

    public List<String> readOnlinePlayers(Path latestLogPath) {
        if (!Files.exists(latestLogPath)) {
            return List.of();
        }

        try {
            List<String> lines = Files.readAllLines(latestLogPath, StandardCharsets.UTF_8);
            Set<String> players = new LinkedHashSet<>();
            for (String line : lines) {
                Matcher joinMatcher = PLAYER_JOIN_PATTERN.matcher(line);
                if (joinMatcher.find()) {
                    players.add(joinMatcher.group(1));
                }

                Matcher leaveMatcher = PLAYER_LEAVE_PATTERN.matcher(line);
                if (leaveMatcher.find()) {
                    players.remove(leaveMatcher.group(1));
                }
            }
            return List.copyOf(players);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect latest.log for players.", exception);
        }
    }

    public double readProcessCpuLoad(ServerRuntimeState.RuntimeContext context) {
        if (context.getProcess() == null) {
            return 0.0d;
        }

        Optional<Duration> cpuDuration = context.getProcess().info().totalCpuDuration();
        if (cpuDuration.isEmpty()) {
            return 0.0d;
        }

        long now = System.nanoTime();
        long cpuNanos = cpuDuration.get().toNanos();
        if (context.getLastCpuSampleNanos() == 0L) {
            context.setLastCpuSampleNanos(now);
            context.setLastCpuDurationNanos(cpuNanos);
            return 0.0d;
        }

        long elapsedWallNanos = now - context.getLastCpuSampleNanos();
        long elapsedCpuNanos = cpuNanos - context.getLastCpuDurationNanos();
        context.setLastCpuSampleNanos(now);
        context.setLastCpuDurationNanos(cpuNanos);

        if (elapsedWallNanos <= 0L) {
            return 0.0d;
        }

        double usage = (elapsedCpuNanos * 100.0d) / (elapsedWallNanos * Runtime.getRuntime().availableProcessors());
        return Math.max(0.0d, usage);
    }

    public long readProcessMemoryBytes(long pid) {
        try {
            Process probe = new ProcessBuilder(
                    "tasklist",
                    "/FI",
                    "PID eq " + pid,
                    "/FO",
                    "CSV",
                    "/NH"
            ).redirectErrorStream(true).start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(probe.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line == null || line.startsWith("INFO:")) {
                    return 0L;
                }

                String[] parts = parseCsvLine(line);
                if (parts.length < 5) {
                    return 0L;
                }

                String mem = parts[4].replace("\"", "").replace(",", "").replace(" K", "").trim();
                return Long.parseLong(mem) * 1024L;
            }
        } catch (Exception exception) {
            return 0L;
        }
    }

    public String[] parseCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                inQuotes = !inQuotes;
                current.append(currentChar);
            } else if (currentChar == ',' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        parts.add(current.toString());
        return parts.toArray(String[]::new);
    }

    public String defaultServerProperties(String minecraftDirectory, String requested) {
        if (!isBlank(requested)) {
            return requested.trim();
        }
        if (!isBlank(minecraftDirectory)) {
            Path file = Path.of(minecraftDirectory).resolve("server.properties");
            return readOptionalFile(file);
        }
        return "";
    }

    public void startOutputPump(long serverId, ManagedServer server, ServerRuntimeState.RuntimeContext context) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    context.getProcess().getInputStream(),
                    StandardCharsets.UTF_8
            ))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (context.getConsoleLines()) {
                        context.getConsoleLines().addLast(line);
                        while (context.getConsoleLines().size() > MAX_CONSOLE_LINES) {
                            context.getConsoleLines().removeFirst();
                        }
                    }
                }
            } catch (IOException ignored) {
                // Process shutdown closes the stream.
            } finally {
                if (!context.isManuallyStopping()) {
                    finalizeStoppedServer(serverId, server);
                }
            }
        }, "mc-console-" + serverId);
        thread.setDaemon(true);
        context.setOutputThread(thread);
        thread.start();
    }

    public void finalizeStoppedServer(long serverId, ManagedServer server) {
        ServerRuntimeState.RuntimeContext context = runtimeState.getRuntimeContexts().remove(serverId);
        if (context != null) {
            context.setManuallyStopping(true);
            try {
                context.getInput().close();
            } catch (IOException ignored) {
            }
        }

        server.setStatus(isBlank(server.getMinecraftDirectory()) ? ServerStatus.STARTUP : ServerStatus.STOPPED);
        serverStore.save(server);
    }

    public boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private int estimateLineIndex(List<String> lines, long byteOffset) {
        long running = 0L;
        for (int index = 0; index < lines.size(); index++) {
            running += lines.get(index).getBytes(StandardCharsets.UTF_8).length + 2L;
            if (running > byteOffset) {
                return index;
            }
        }
        return lines.size();
    }
}
