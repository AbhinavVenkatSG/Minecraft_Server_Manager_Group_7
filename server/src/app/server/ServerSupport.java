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
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.OptionalDouble;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helper methods for server persistence, filesystem access, log parsing, and process telemetry.
 */
public class ServerSupport {
    private static final int MAX_CONSOLE_LINES = 250;
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile(": ([A-Za-z0-9_]{1,16}) joined the game");
    private static final Pattern PLAYER_LEAVE_PATTERN = Pattern.compile(": ([A-Za-z0-9_]{1,16}) left the game");

    private final ServerStore serverStore;
    private final ServerRuntimeState runtimeState;

    /**
     * Creates the shared helper facade used by the server services.
     *
     * @param serverStore persistent managed server storage
     * @param runtimeState in-memory runtime state
     */
    public ServerSupport(ServerStore serverStore, ServerRuntimeState runtimeState) {
        this.serverStore = serverStore;
        this.runtimeState = runtimeState;
    }

    /**
     * Returns the underlying persistent server store.
     *
     * @return server store implementation
     */
    public ServerStore getServerStore() {
        return serverStore;
    }

    /**
     * Returns the in-memory runtime state for live processes and telemetry caches.
     *
     * @return runtime state container
     */
    public ServerRuntimeState getRuntimeState() {
        return runtimeState;
    }

    /**
     * Lists all persisted servers.
     *
     * @return known servers
     */
    public List<ManagedServer> listServers() {
        return serverStore.findAll();
    }

    /**
     * Finds a server by id.
     *
     * @param serverId managed server id
     * @return the server when it exists
     */
    public Optional<ManagedServer> getServer(long serverId) {
        return serverStore.findById(serverId);
    }

    /**
     * Persists the supplied server record.
     *
     * @param server server to save
     * @return the saved server
     */
    public ManagedServer saveServer(ManagedServer server) {
        return serverStore.save(server);
    }

    /**
     * Loads a server or throws when the id is unknown.
     *
     * @param serverId managed server id
     * @return the stored server
     */
    public ManagedServer requireServer(long serverId) {
        return serverStore.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found."));
    }

    /**
     * Converts persisted running states back to a safe startup or stopped state after a restart.
     */
    public void normalizePersistedState() {
        for (ManagedServer server : serverStore.findAll()) {
            if (server.getStatus() == ServerStatus.RUNNING || server.getStatus() == ServerStatus.BLOCKED) {
                server.setStatus(isBlank(server.getMinecraftDirectory()) ? ServerStatus.STARTUP : ServerStatus.STOPPED);
                serverStore.save(server);
            }
        }
    }

    /**
     * Verifies that a directory exists and looks like a Minecraft server folder.
     *
     * @param directory candidate Minecraft directory
     * @return normalized absolute directory path
     */
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

    /**
     * Returns the configured Minecraft directory for a server, or throws when unset.
     *
     * @param server managed server record
     * @return validated Minecraft directory
     */
    public Path requireMinecraftDirectory(ManagedServer server) {
        if (isBlank(server.getMinecraftDirectory())) {
            throw new IllegalStateException("Minecraft directory has not been configured.");
        }
        return validateMinecraftDirectory(Path.of(server.getMinecraftDirectory()));
    }

    /**
     * Resolves the batch script used to start the Minecraft server.
     *
     * @param minecraftDirectory Minecraft directory
     * @return matching start script path
     */
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

    /**
     * Resolves the whitelist file, preferring JSON when both formats are possible.
     *
     * @param minecraftDirectory Minecraft directory
     * @return whitelist file path
     */
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

    /**
     * Blocks edits while the server is running or transitioning.
     *
     * @param server managed server record
     */
    public void ensureServerStopped(ManagedServer server) {
        if (server.getStatus() == ServerStatus.RUNNING || server.getStatus() == ServerStatus.BLOCKED) {
            throw new IllegalStateException("Stop the server before editing files.");
        }
    }

    /**
     * Reads a file when it exists, otherwise returns an empty string.
     *
     * @param path file path to inspect
     * @return file contents or an empty string
     */
    public String readOptionalFile(Path path) {
        return Files.exists(path) ? readRequiredFile(path) : "";
    }

    /**
     * Reads a UTF-8 text file or throws a service-level error.
     *
     * @param path file path to read
     * @return file contents
     */
    public String readRequiredFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read file: " + path, exception);
        }
    }

    /**
     * Writes a UTF-8 text file, creating parent directories when needed.
     *
     * @param path output file path
     * @param content text to write
     */
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

    /**
     * Computes the total size of every regular file in a directory tree.
     *
     * @param root directory root
     * @return total size in bytes
     */
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

    /**
     * Reads total and usable disk space for the filesystem containing the path.
     *
     * @param path path on the target filesystem
     * @return array of {@code [totalBytes, usableBytes]}
     */
    public long[] getDriveMetrics(Path path) {
        try {
            FileStore fileStore = Files.getFileStore(path);
            return new long[] {fileStore.getTotalSpace(), fileStore.getUsableSpace()};
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to measure available drive space.", exception);
        }
    }

    /**
     * Reads only the log lines appended since the last snapshot for the server.
     *
     * @param serverId managed server id
     * @param latestLogPath path to {@code latest.log}
     * @return new log lines since the last read
     */
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

    /**
     * Replays the latest log to derive the set of currently online players.
     *
     * @param latestLogPath path to {@code latest.log}
     * @return player names that appear to still be online
     */
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

    /**
     * Chooses the live process that best represents the actual Minecraft JVM for telemetry.
     *
     * @param context tracked runtime context
     * @return the most relevant process handle when one can be identified
     */
    public Optional<ProcessHandle> resolveTelemetryProcess(ServerRuntimeState.RuntimeContext context) {
        if (context == null || context.getProcess() == null) {
            return Optional.empty();
        }

        ProcessHandle root = context.getProcess().toHandle();
        if (!root.isAlive()) {
            return Optional.empty();
        }

        List<ProcessHandle> liveDescendants = root.descendants()
                .filter(ProcessHandle::isAlive)
                .sorted(Comparator.comparingLong(this::telemetryProcessPriority).reversed())
                .toList();
        if (!liveDescendants.isEmpty()) {
            return Optional.of(liveDescendants.get(0));
        }

        return Optional.of(root);
    }

    /**
     * Computes CPU usage from two successive process samples.
     *
     * @param context tracked runtime context
     * @return CPU load percent when enough sampling data exists
     */
    public OptionalDouble readProcessCpuLoad(ServerRuntimeState.RuntimeContext context) {
        if (context.getProcess() == null) {
            return OptionalDouble.empty();
        }

        Optional<ProcessHandle> telemetryProcess = resolveTelemetryProcess(context);
        if (telemetryProcess.isEmpty()) {
            return OptionalDouble.empty();
        }

        ProcessHandle processHandle = telemetryProcess.get();
        Optional<Duration> cpuDuration = processHandle.info().totalCpuDuration();
        if (cpuDuration.isEmpty()) {
            return OptionalDouble.empty();
        }

        // ProcessHandle only gives cumulative CPU time, so usage has to be derived from deltas.
        long now = System.nanoTime();
        long pid = processHandle.pid();
        long cpuNanos = cpuDuration.get().toNanos();
        if (context.getLastCpuSampleNanos() == 0L || context.getLastCpuSampledPid() != pid) {
            context.setLastCpuSampleNanos(now);
            context.setLastCpuDurationNanos(cpuNanos);
            context.setLastCpuSampledPid(pid);
            return OptionalDouble.empty();
        }

        long elapsedWallNanos = now - context.getLastCpuSampleNanos();
        long elapsedCpuNanos = cpuNanos - context.getLastCpuDurationNanos();
        context.setLastCpuSampleNanos(now);
        context.setLastCpuDurationNanos(cpuNanos);
        context.setLastCpuSampledPid(pid);

        if (elapsedWallNanos <= 0L || elapsedCpuNanos < 0L) {
            return OptionalDouble.empty();
        }

        double usage = (elapsedCpuNanos * 100.0d) / (elapsedWallNanos * Runtime.getRuntime().availableProcessors());
        if (Double.isNaN(usage) || Double.isInfinite(usage)) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(Math.max(0.0d, usage));
    }

    /**
     * Reads memory usage for the live telemetry process.
     *
     * @param context tracked runtime context
     * @return memory usage in bytes
     */
    public long readProcessMemoryBytes(ServerRuntimeState.RuntimeContext context) {
        Optional<ProcessHandle> telemetryProcess = resolveTelemetryProcess(context);
        if (telemetryProcess.isEmpty()) {
            return 0L;
        }

        return readProcessMemoryBytes(telemetryProcess.get().pid());
    }

    /**
     * Reads memory usage for a Windows process via {@code tasklist}.
     *
     * @param pid process id
     * @return memory usage in bytes, or zero when it cannot be measured
     */
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

    /**
     * Splits a CSV line while preserving quoted commas.
     *
     * @param line raw CSV line
     * @return parsed columns
     */
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

    /**
     * Chooses the server properties content supplied during server creation.
     *
     * @param minecraftDirectory configured Minecraft directory, if any
     * @param requested request payload contents
     * @return explicit request contents, disk contents, or an empty string
     */
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

    /**
     * Starts a daemon thread that mirrors the process output into the console buffer.
     *
     * @param serverId managed server id
     * @param server managed server record
     * @param context tracked runtime context
     */
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

    /**
     * Clears runtime state and persists a safe stopped status after process exit.
     *
     * @param serverId managed server id
     * @param server managed server record
     */
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

    /**
     * Checks whether a string is null, empty, or whitespace only.
     *
     * @param value candidate text
     * @return true when the text is blank
     */
    public boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Trims a string and collapses nulls to an empty string.
     *
     * @param value candidate text
     * @return safe trimmed text
     */
    public String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Rounds a floating-point value to two decimal places.
     *
     * @param value source number
     * @return rounded number
     */
    public double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private long telemetryProcessPriority(ProcessHandle processHandle) {
        long score = 0L;
        String command = processHandle.info().command().orElse("").toLowerCase();
        if (command.endsWith("java.exe") || command.endsWith("javaw.exe") || command.endsWith("/java") || command.endsWith("/javaw")) {
            score += 1_000_000L;
        }

        return score + processHandle.pid();
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
