package domain.server;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ServerStatusTest {

    @Test
    void RUNNING_enumValue_exists() {
        ServerStatus status = ServerStatus.RUNNING;
        assertEquals("RUNNING", status.name());
    }

    @Test
    void STOPPED_enumValue_exists() {
        ServerStatus status = ServerStatus.STOPPED;
        assertEquals("STOPPED", status.name());
    }

    @Test
    void allStatuses_haveCorrectNames() {
        assertEquals(4, ServerStatus.values().length);
        assertEquals("RUNNING", ServerStatus.RUNNING.name());
        assertEquals("STOPPED", ServerStatus.STOPPED.name());
        assertEquals("STARTUP", ServerStatus.STARTUP.name());
        assertEquals("BLOCKED", ServerStatus.BLOCKED.name());
    }

    @Test
    void valueOf_returnsCorrectStatus() {
        assertEquals(ServerStatus.RUNNING, ServerStatus.valueOf("RUNNING"));
        assertEquals(ServerStatus.STOPPED, ServerStatus.valueOf("STOPPED"));
    }

    @Test
    void valueOf_withInvalidName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ServerStatus.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> ServerStatus.valueOf("running"));
    }
}
