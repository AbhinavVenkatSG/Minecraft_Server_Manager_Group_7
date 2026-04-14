package domain.server;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ManagedServerTest {

    @Test
    void constructor_withAllFields_setsFieldsCorrectly() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime lastStarted = LocalDateTime.of(2024, 1, 15, 10, 30);
        ManagedServer server = new ManagedServer(
            1L, "MyServer", "localhost", 25565, "rconpass", 25575,
            ServerStatus.RUNNING, "properties", "/backups", "/mc/server",
            10L, createdAt, lastStarted
        );

        assertEquals(1L, server.getId());
        assertEquals("MyServer", server.getName());
        assertEquals("localhost", server.getHost());
        assertEquals(25565, server.getPort());
        assertEquals("rconpass", server.getRconPassword());
        assertEquals(25575, server.getRconPort());
        assertEquals(ServerStatus.RUNNING, server.getStatus());
        assertEquals("properties", server.getServerProperties());
        assertEquals("/backups", server.getBackupPath());
        assertEquals("/mc/server", server.getMinecraftDirectory());
        assertEquals(10L, server.getOwnerId());
        assertEquals(createdAt, server.getCreatedAt());
        assertEquals(lastStarted, server.getLastStarted());
    }

    @Test
    void setId_updatesId() {
        ManagedServer server = createTestServer();
        server.setId(99L);
        assertEquals(99L, server.getId());
    }

    @Test
    void setStatus_updatesStatus() {
        ManagedServer server = createTestServer();
        assertEquals(ServerStatus.STOPPED, server.getStatus());

        server.setStatus(ServerStatus.RUNNING);
        assertEquals(ServerStatus.RUNNING, server.getStatus());

        server.setStatus(ServerStatus.STOPPED);
        assertEquals(ServerStatus.STOPPED, server.getStatus());
    }

    @Test
    void setLastStarted_updatesLastStarted() {
        ManagedServer server = createTestServer();
        assertNull(server.getLastStarted());

        LocalDateTime newTime = LocalDateTime.of(2024, 6, 15, 14, 30);
        server.setLastStarted(newTime);
        assertEquals(newTime, server.getLastStarted());
    }

    @Test
    void getLastStarted_whenNeverStarted_returnsNull() {
        ManagedServer server = new ManagedServer(
            1L, "Test", "host", 25565, "pass", 25575,
            ServerStatus.STOPPED, null, null, null, 1L, LocalDateTime.now(), null
        );
        assertNull(server.getLastStarted());
    }

    @Test
    void immutableFields_remainUnchanged() {
        ManagedServer server = createTestServer();
        assertEquals("MyServer", server.getName());
        assertEquals("localhost", server.getHost());
        assertEquals(25565, server.getPort());
        assertEquals("rconpass", server.getRconPassword());
        assertEquals(25575, server.getRconPort());
        assertEquals("props", server.getServerProperties());
        assertEquals("/backup/path", server.getBackupPath());
        assertEquals("/mc/dir", server.getMinecraftDirectory());
        assertEquals(5L, server.getOwnerId());
    }

    @Test
    void constructor_withMinimalFields_worksCorrectly() {
        ManagedServer server = new ManagedServer(
            0L, "", "", 0, "", 0,
            ServerStatus.STOPPED, "", "", "", 0L, null, null
        );

        assertEquals(0L, server.getId());
        assertEquals("", server.getName());
        assertEquals("", server.getHost());
        assertEquals(0, server.getPort());
        assertEquals("", server.getRconPassword());
        assertEquals(0, server.getRconPort());
        assertEquals(ServerStatus.STOPPED, server.getStatus());
        assertEquals("", server.getServerProperties());
        assertEquals("", server.getBackupPath());
        assertEquals("", server.getMinecraftDirectory());
        assertEquals(0L, server.getOwnerId());
        assertNull(server.getCreatedAt());
        assertNull(server.getLastStarted());
    }

    @Test
    void statusTransitions_workCorrectly() {
        ManagedServer server = createTestServer();

        assertEquals(ServerStatus.STOPPED, server.getStatus());

        server.setStatus(ServerStatus.RUNNING);
        assertEquals(ServerStatus.RUNNING, server.getStatus());

        server.setLastStarted(LocalDateTime.now());
        assertNotNull(server.getLastStarted());

        server.setStatus(ServerStatus.STOPPED);
        assertEquals(ServerStatus.STOPPED, server.getStatus());
    }

    private ManagedServer createTestServer() {
        return new ManagedServer(
            1L, "MyServer", "localhost", 25565, "rconpass", 25575,
            ServerStatus.STOPPED, "props", "/backup/path", "/mc/dir", 5L,
            LocalDateTime.of(2024, 1, 1, 12, 0), null
        );
    }
}
