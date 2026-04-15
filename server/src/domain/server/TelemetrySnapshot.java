/**
 * @file TelemetrySnapshot.java
 * @brief Domain entity containing server telemetry data at a point in time.
 * @{
 */

package domain.server;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @class TelemetrySnapshot
 * @brief Immutable snapshot of server runtime telemetry.
 * @details Captures CPU, memory, disk, player, and log information.
 */
public class TelemetrySnapshot {
    private final long serverId;
    private final ServerStatus operationalState;
    private final Double systemCpuLoadPercent;
    private final long systemMemoryUsedBytes;
    private final long systemMemoryTotalBytes;
    private final long minecraftProcessMemoryBytes;
    private final Double minecraftProcessCpuLoadPercent;
    private final String jvmAllocatedRam;
    private final String jvmInitialRam;
    private final long minecraftDirectorySizeBytes;
    private final long driveTotalBytes;
    private final long driveUsableBytes;
    private final int playerCount;
    private final List<String> onlinePlayers;
    private final List<String> newLogLines;
    private final String packetBase64;
    private final LocalDateTime capturedAt;

    public TelemetrySnapshot(
            long serverId,
            ServerStatus operationalState,
            Double systemCpuLoadPercent,
            long systemMemoryUsedBytes,
            long systemMemoryTotalBytes,
            long minecraftProcessMemoryBytes,
            Double minecraftProcessCpuLoadPercent,
            String jvmAllocatedRam,
            String jvmInitialRam,
            long minecraftDirectorySizeBytes,
            long driveTotalBytes,
            long driveUsableBytes,
            int playerCount,
            List<String> onlinePlayers,
            List<String> newLogLines,
            String packetBase64,
            LocalDateTime capturedAt
    ) {
        this.serverId = serverId;
        this.operationalState = operationalState;
        this.systemCpuLoadPercent = systemCpuLoadPercent;
        this.systemMemoryUsedBytes = systemMemoryUsedBytes;
        this.systemMemoryTotalBytes = systemMemoryTotalBytes;
        this.minecraftProcessMemoryBytes = minecraftProcessMemoryBytes;
        this.minecraftProcessCpuLoadPercent = minecraftProcessCpuLoadPercent;
        this.jvmAllocatedRam = jvmAllocatedRam;
        this.jvmInitialRam = jvmInitialRam;
        this.minecraftDirectorySizeBytes = minecraftDirectorySizeBytes;
        this.driveTotalBytes = driveTotalBytes;
        this.driveUsableBytes = driveUsableBytes;
        this.playerCount = playerCount;
        this.onlinePlayers = List.copyOf(onlinePlayers);
        this.newLogLines = List.copyOf(newLogLines);
        this.packetBase64 = packetBase64;
        this.capturedAt = capturedAt;
    }

    public long getServerId() {
        return serverId;
    }

    public ServerStatus getOperationalState() {
        return operationalState;
    }

    public Double getSystemCpuLoadPercent() {
        return systemCpuLoadPercent;
    }

    public long getSystemMemoryUsedBytes() {
        return systemMemoryUsedBytes;
    }

    public long getSystemMemoryTotalBytes() {
        return systemMemoryTotalBytes;
    }

    public long getMinecraftProcessMemoryBytes() {
        return minecraftProcessMemoryBytes;
    }

    public Double getMinecraftProcessCpuLoadPercent() {
        return minecraftProcessCpuLoadPercent;
    }

    public String getJvmAllocatedRam() {
        return jvmAllocatedRam;
    }

    public String getJvmInitialRam() {
        return jvmInitialRam;
    }

    public long getMinecraftDirectorySizeBytes() {
        return minecraftDirectorySizeBytes;
    }

    public long getDriveTotalBytes() {
        return driveTotalBytes;
    }

    public long getDriveUsableBytes() {
        return driveUsableBytes;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public List<String> getOnlinePlayers() {
        return onlinePlayers;
    }

    public List<String> getNewLogLines() {
        return newLogLines;
    }

    public String getPacketBase64() {
        return packetBase64;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }
}
