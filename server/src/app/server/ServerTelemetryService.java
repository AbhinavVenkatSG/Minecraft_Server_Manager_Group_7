package app.server;

import com.sun.management.OperatingSystemMXBean;
import core.protocol.PacketBuilder;
import domain.server.ManagedServer;
import domain.server.TelemetrySnapshot;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerTelemetryService {
    private static final int MAX_TELEMETRY_HISTORY = 50;
    private static final Pattern XMX_PATTERN = Pattern.compile("(?i)-Xmx(\\S+)");
    private static final Pattern XMS_PATTERN = Pattern.compile("(?i)-Xms(\\S+)");

    private final ServerSupport support;
    private final ScheduledExecutorService telemetryScheduler = Executors.newSingleThreadScheduledExecutor();

    public ServerTelemetryService(ServerSupport support) {
        this.support = support;
        telemetryScheduler.scheduleAtFixedRate(this::refreshTelemetrySafely, 30, 30, TimeUnit.SECONDS);
    }

    public TelemetrySnapshot getTelemetry(long serverId) {
        ManagedServer server = support.requireServer(serverId);
        TelemetrySnapshot snapshot = collectTelemetry(server);
        support.getRuntimeState().getLatestTelemetry().put(serverId, snapshot);
        return snapshot;
    }

    public Optional<TelemetrySnapshot> getLatestTelemetry(long serverId) {
        return Optional.ofNullable(support.getRuntimeState().getLatestTelemetry().get(serverId));
    }

    public List<String> getTelemetryHistory(long serverId) {
        Deque<String> history = support.getRuntimeState().getTelemetryHistory().get(serverId);
        if (history == null) {
            return List.of();
        }

        synchronized (history) {
            return List.copyOf(history);
        }
    }

    public void shutdown() {
        telemetryScheduler.shutdownNow();
    }

    private void refreshTelemetrySafely() {
        for (ManagedServer server : support.listServers()) {
            try {
                support.getRuntimeState().getLatestTelemetry().put(server.getId(), collectTelemetry(server));
            } catch (Exception ignored) {
                // Keep the scheduler alive even when a directory or process is temporarily invalid.
            }
        }
    }

    private TelemetrySnapshot collectTelemetry(ManagedServer server) {
        long directorySize = 0L;
        long driveTotal = 0L;
        long driveUsable = 0L;
        long processMemory = 0L;
        double processCpuLoad = 0.0d;
        String jvmAllocatedRam = "";
        String jvmInitialRam = "";
        List<String> newLogLines = List.of();
        List<String> onlinePlayers = List.of();

        if (!support.isBlank(server.getMinecraftDirectory())) {
            Path minecraftDirectory = support.requireMinecraftDirectory(server);
            directorySize = support.computeDirectorySize(minecraftDirectory);
            long[] driveMetrics = support.getDriveMetrics(minecraftDirectory);
            driveTotal = driveMetrics[0];
            driveUsable = driveMetrics[1];
            String startParameters = "";
            try {
                startParameters = support.readRequiredFile(support.resolveStartScript(minecraftDirectory));
            } catch (IllegalStateException ignored) {
                startParameters = "";
            }
            jvmAllocatedRam = extractJvmArgument(startParameters, XMX_PATTERN);
            jvmInitialRam = extractJvmArgument(startParameters, XMS_PATTERN);
            newLogLines = support.readNewLogLines(server.getId(), minecraftDirectory.resolve("logs").resolve("latest.log"));
            onlinePlayers = support.readOnlinePlayers(minecraftDirectory.resolve("logs").resolve("latest.log"));
        }

        ServerRuntimeState.RuntimeContext context = support.getRuntimeState().getRuntimeContexts().get(server.getId());
        if (context != null && context.getProcess().isAlive()) {
            processCpuLoad = support.readProcessCpuLoad(context);
            processMemory = support.readProcessMemoryBytes(context.getProcess().pid());
        }

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        double systemCpuLoad = Math.max(0.0d, osBean.getCpuLoad() * 100.0d);

        String payload = buildTelemetryPayload(
                server,
                systemCpuLoad,
                totalMemory - freeMemory,
                totalMemory,
                processMemory,
                processCpuLoad,
                jvmAllocatedRam,
                jvmInitialRam,
                directorySize,
                driveTotal,
                driveUsable,
                onlinePlayers,
                newLogLines
        );

        String packetBase64 = Base64.getEncoder().encodeToString(PacketBuilder.buildTelemetry(payload).toBytes());
        TelemetrySnapshot snapshot = new TelemetrySnapshot(
                server.getId(),
                server.getStatus(),
                systemCpuLoad,
                totalMemory - freeMemory,
                totalMemory,
                processMemory,
                processCpuLoad,
                jvmAllocatedRam,
                jvmInitialRam,
                directorySize,
                driveTotal,
                driveUsable,
                onlinePlayers.size(),
                onlinePlayers,
                newLogLines,
                packetBase64,
                LocalDateTime.now()
        );
        appendTelemetryHistory(server.getId(), snapshot);
        return snapshot;
    }

    private String buildTelemetryPayload(
            ManagedServer server,
            double systemCpuLoad,
            long systemMemoryUsed,
            long systemMemoryTotal,
            long processMemory,
            double processCpuLoad,
            String jvmAllocatedRam,
            String jvmInitialRam,
            long directorySize,
            long driveTotal,
            long driveUsable,
            List<String> onlinePlayers,
            List<String> newLogLines
    ) {
        return "serverId=" + server.getId()
                + ";state=" + server.getStatus().name()
                + ";systemCpuLoadPercent=" + support.round(systemCpuLoad)
                + ";systemMemoryUsedBytes=" + systemMemoryUsed
                + ";systemMemoryTotalBytes=" + systemMemoryTotal
                + ";minecraftProcessMemoryBytes=" + processMemory
                + ";minecraftProcessCpuLoadPercent=" + support.round(processCpuLoad)
                + ";jvmAllocatedRam=" + jvmAllocatedRam
                + ";jvmInitialRam=" + jvmInitialRam
                + ";minecraftDirectorySizeBytes=" + directorySize
                + ";driveTotalBytes=" + driveTotal
                + ";driveUsableBytes=" + driveUsable
                + ";playerCount=" + onlinePlayers.size()
                + ";onlinePlayers=" + String.join(",", onlinePlayers)
                + ";newLogLines=" + String.join(" | ", newLogLines);
    }

    private String extractJvmArgument(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private void appendTelemetryHistory(long serverId, TelemetrySnapshot snapshot) {
        Deque<String> history = support.getRuntimeState().getTelemetryHistory().computeIfAbsent(serverId, unused -> new ArrayDeque<>());
        String entry = snapshot.getCapturedAt()
                + " | state=" + snapshot.getOperationalState().name()
                + " | cpu=" + support.round(snapshot.getMinecraftProcessCpuLoadPercent()) + "%"
                + " | ram=" + snapshot.getMinecraftProcessMemoryBytes() + " bytes"
                + " | players=" + snapshot.getPlayerCount()
                + " | size=" + snapshot.getMinecraftDirectorySizeBytes() + " bytes"
                + " | logs=" + snapshot.getNewLogLines().size();

        synchronized (history) {
            history.addLast(entry);
            while (history.size() > MAX_TELEMETRY_HISTORY) {
                history.removeFirst();
            }
        }
    }
}
