package infra.http;

import app.auth.dto.LoginResponse;
import domain.backup.Backup;
import domain.server.ManagedServer;
import domain.server.TelemetrySnapshot;

import java.util.List;

public final class Responses {
    private Responses() {
    }

    public static String message(String message) {
        return "{\"message\":" + Json.quote(message) + "}";
    }

    public static String login(LoginResponse response) {
        return "{"
                + "\"token\":" + Json.quote(response.getToken()) + ","
                + "\"userId\":" + response.getUserId() + ","
                + "\"username\":" + Json.quote(response.getUsername())
                + "}";
    }

    public static String server(ManagedServer server) {
        return "{"
                + "\"id\":" + server.getId() + ","
                + "\"name\":" + Json.quote(server.getName()) + ","
                + "\"host\":" + Json.quote(server.getHost()) + ","
                + "\"port\":" + server.getPort() + ","
                + "\"rconPort\":" + server.getRconPort() + ","
                + "\"status\":" + Json.quote(server.getStatus().name()) + ","
                + "\"serverProperties\":" + Json.quote(server.getServerProperties()) + ","
                + "\"backupPath\":" + Json.quote(server.getBackupPath()) + ","
                + "\"minecraftDirectory\":" + Json.quote(server.getMinecraftDirectory()) + ","
                + "\"ownerId\":" + server.getOwnerId() + ","
                + "\"createdAt\":" + Json.quote(server.getCreatedAt().toString()) + ","
                + "\"lastStarted\":" + Json.quote(server.getLastStarted() == null ? "" : server.getLastStarted().toString())
                + "}";
    }

    public static String servers(List<ManagedServer> servers) {
        return array(servers.stream().map(Responses::server).toList());
    }

    public static String backup(Backup backup) {
        return "{"
                + "\"id\":" + backup.getId() + ","
                + "\"serverId\":" + backup.getServerId() + ","
                + "\"filename\":" + Json.quote(backup.getFilename()) + ","
                + "\"filePath\":" + Json.quote(backup.getFilePath()) + ","
                + "\"fileSize\":" + backup.getFileSize() + ","
                + "\"createdAt\":" + Json.quote(backup.getCreatedAt().toString())
                + "}";
    }

    public static String backups(List<Backup> backups) {
        return array(backups.stream().map(Responses::backup).toList());
    }

    public static String players(List<String> players) {
        return array(players.stream().map(Json::quote).toList());
    }

    public static String stringArray(String key, List<String> values) {
        return "{"
                + Json.quote(key) + ":" + array(values.stream().map(Json::quote).toList())
                + "}";
    }

    public static String textPayload(String key, String value) {
        return "{"
                + Json.quote(key) + ":" + Json.quote(value == null ? "" : value)
                + "}";
    }

    public static String telemetry(TelemetrySnapshot snapshot) {
        return "{"
                + "\"serverId\":" + snapshot.getServerId() + ","
                + "\"operationalState\":" + Json.quote(snapshot.getOperationalState().name()) + ","
                + "\"systemCpuLoadPercent\":" + snapshot.getSystemCpuLoadPercent() + ","
                + "\"systemMemoryUsedBytes\":" + snapshot.getSystemMemoryUsedBytes() + ","
                + "\"systemMemoryTotalBytes\":" + snapshot.getSystemMemoryTotalBytes() + ","
                + "\"minecraftProcessMemoryBytes\":" + snapshot.getMinecraftProcessMemoryBytes() + ","
                + "\"minecraftProcessCpuLoadPercent\":" + snapshot.getMinecraftProcessCpuLoadPercent() + ","
                + "\"jvmAllocatedRam\":" + Json.quote(snapshot.getJvmAllocatedRam()) + ","
                + "\"jvmInitialRam\":" + Json.quote(snapshot.getJvmInitialRam()) + ","
                + "\"minecraftDirectorySizeBytes\":" + snapshot.getMinecraftDirectorySizeBytes() + ","
                + "\"driveTotalBytes\":" + snapshot.getDriveTotalBytes() + ","
                + "\"driveUsableBytes\":" + snapshot.getDriveUsableBytes() + ","
                + "\"playerCount\":" + snapshot.getPlayerCount() + ","
                + "\"onlinePlayers\":" + array(snapshot.getOnlinePlayers().stream().map(Json::quote).toList()) + ","
                + "\"newLogLines\":" + array(snapshot.getNewLogLines().stream().map(Json::quote).toList()) + ","
                + "\"packetBase64\":" + Json.quote(snapshot.getPacketBase64()) + ","
                + "\"capturedAt\":" + Json.quote(snapshot.getCapturedAt().toString())
                + "}";
    }

    private static String array(List<String> items) {
        return "[" + String.join(",", items) + "]";
    }
}
