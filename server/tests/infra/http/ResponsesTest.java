package infra.http;

import app.auth.dto.LoginResponse;
import domain.backup.Backup;
import domain.server.ManagedServer;
import domain.server.ServerStatus;
import domain.user.User;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ResponsesTest {

    @Test
    void message_withString_returnsJsonMessage() {
        String result = Responses.message("Success");
        assertTrue(result.contains("\"message\""));
        assertTrue(result.contains("\"Success\""));
    }

    @Test
    void message_withEmptyString_returnsValidJson() {
        String result = Responses.message("");
        assertEquals("{\"message\":\"\"}", result);
    }

    @Test
    void login_withResponse_returnsCorrectJson() {
        LoginResponse response = new LoginResponse("token123", 1L, "testuser");
        String result = Responses.login(response);

        assertTrue(result.contains("\"token\":\"token123\""));
        assertTrue(result.contains("\"userId\":1"));
        assertTrue(result.contains("\"username\":\"testuser\""));
    }

    @Test
    void server_withRunningStatus_returnsCorrectJson() {
        LocalDateTime created = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime lastStarted = LocalDateTime.of(2024, 1, 15, 10, 0);
        ManagedServer server = new ManagedServer(
            1L, "MyServer", "localhost", 25565, "pass", 25575,
            ServerStatus.RUNNING, "difficulty=hard", "/backups", "/mc/server",
            5L, created, lastStarted
        );

        String result = Responses.server(server);

        assertTrue(result.contains("\"id\":1"));
        assertTrue(result.contains("\"name\":\"MyServer\""));
        assertTrue(result.contains("\"host\":\"localhost\""));
        assertTrue(result.contains("\"port\":25565"));
        assertTrue(result.contains("\"rconPort\":25575"));
        assertTrue(result.contains("\"status\":\"RUNNING\""));
        assertTrue(result.contains("\"ownerId\":5"));
        assertTrue(result.contains("\"createdAt\":"));
        assertTrue(result.contains("\"lastStarted\":"));
    }

    @Test
    void server_withNullLastStarted_returnsEmptyString() {
        ManagedServer server = new ManagedServer(
            1L, "Test", "host", 25565, "pass", 25575,
            ServerStatus.STOPPED, null, null, null, 1L, LocalDateTime.now(), null
        );

        String result = Responses.server(server);
        assertTrue(result.contains("\"lastStarted\":\"\""));
    }

    @Test
    void servers_withEmptyList_returnsEmptyArray() {
        String result = Responses.servers(Collections.emptyList());
        assertEquals("[]", result);
    }

    @Test
    void servers_withMultipleServers_returnsArrayOfServers() {
        ManagedServer server1 = createServer(1L, "Server1");
        ManagedServer server2 = createServer(2L, "Server2");

        String result = Responses.servers(Arrays.asList(server1, server2));

        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("\"name\":\"Server1\""));
        assertTrue(result.contains("\"name\":\"Server2\""));
    }

    @Test
    void backup_withAllFields_returnsCorrectJson() {
        LocalDateTime created = LocalDateTime.of(2024, 3, 15, 14, 30);
        Backup backup = new Backup(1L, 10L, "world.zip", "/backups/10/world.zip", 44040192L, created);

        String result = Responses.backup(backup);

        assertTrue(result.contains("\"id\":1"));
        assertTrue(result.contains("\"serverId\":10"));
        assertTrue(result.contains("\"filename\":\"world.zip\""));
        assertTrue(result.contains("\"filePath\":\"/backups/10/world.zip\""));
        assertTrue(result.contains("\"fileSize\":44040192"));
        assertTrue(result.contains("\"createdAt\":"));
    }

    @Test
    void backups_withEmptyList_returnsEmptyArray() {
        String result = Responses.backups(Collections.emptyList());
        assertEquals("[]", result);
    }

    @Test
    void backups_withMultipleBackups_returnsArrayOfBackups() {
        Backup backup1 = createBackup(1L, 10L, "backup1.zip");
        Backup backup2 = createBackup(2L, 10L, "backup2.zip");

        String result = Responses.backups(Arrays.asList(backup1, backup2));

        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("\"filename\":\"backup1.zip\""));
        assertTrue(result.contains("\"filename\":\"backup2.zip\""));
    }

    @Test
    void players_withEmptyList_returnsEmptyArray() {
        String result = Responses.players(Collections.emptyList());
        assertEquals("[]", result);
    }

    @Test
    void players_withMultiplePlayers_returnsArrayOfPlayerNames() {
        List<String> players = Arrays.asList("Steve", "Alex", "Herobrine");
        String result = Responses.players(players);

        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("\"Steve\""));
        assertTrue(result.contains("\"Alex\""));
        assertTrue(result.contains("\"Herobrine\""));
    }

    @Test
    void players_withSpecialCharacters_escapesCorrectly() {
        List<String> players = Arrays.asList("Player \"One\"", "Line1\nLine2");
        String result = Responses.players(players);

        assertTrue(result.contains("\\\""));
        assertTrue(result.contains("\\n"));
    }

    @Test
    void server_withSpecialCharactersInProperties_escapesCorrectly() {
        ManagedServer server = new ManagedServer(
            1L, "Server", "host", 25565, "pass", 25575,
            ServerStatus.RUNNING, "motd=Hello \"World\"\nWelcome!", "/backups", "/mc/server",
            1L, LocalDateTime.now(), null
        );

        String result = Responses.server(server);
        assertTrue(result.contains("\\n"));
        assertTrue(result.contains("\\\""));
    }

    @Test
    void login_withSpecialCharactersInUsername_escapesCorrectly() {
        User user = new User(1L, "user@domain.com", "hash", "salt", "token", LocalDateTime.now());
        LoginResponse response = LoginResponse.fromUser(user);
        String result = Responses.login(response);

        assertTrue(result.contains("\"username\":\"user@domain.com\""));
    }

    private ManagedServer createServer(long id, String name) {
        return new ManagedServer(
            id, name, "localhost", 25565, "pass", 25575,
            ServerStatus.STOPPED, null, null, null, 1L, LocalDateTime.now(), null
        );
    }

    private Backup createBackup(long id, long serverId, String filename) {
        return new Backup(
            id, serverId, filename, "/path/" + filename,
            1000L, LocalDateTime.now()
        );
    }
}
