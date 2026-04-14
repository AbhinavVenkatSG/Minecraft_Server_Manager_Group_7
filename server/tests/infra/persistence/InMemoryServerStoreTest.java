package infra.persistence;

import domain.server.ManagedServer;
import domain.server.ServerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryServerStoreTest {

    private InMemoryServerStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryServerStore();
    }

    @Test
    void save_newServer_assignsIdAndReturnsServer() {
        ManagedServer server = createServer(0L, "TestServer");
        ManagedServer saved = store.save(server);

        assertEquals(1L, saved.getId());
        assertEquals("TestServer", saved.getName());
    }

    @Test
    void save_multipleServers_assignsSequentialIds() {
        ManagedServer server1 = store.save(createServer(0L, "Server1"));
        ManagedServer server2 = store.save(createServer(0L, "Server2"));
        ManagedServer server3 = store.save(createServer(0L, "Server3"));

        assertEquals(1L, server1.getId());
        assertEquals(2L, server2.getId());
        assertEquals(3L, server3.getId());
    }

    @Test
    void save_serverWithExistingId_doesNotChangeId() {
        ManagedServer server = store.save(createServer(99L, "TestServer"));
        assertEquals(99L, server.getId());
    }

    @Test
    void findAll_withNoServers_returnsEmptyList() {
        List<ManagedServer> result = store.findAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_returnsAllServersSortedById() {
        store.save(createServer(3L, "Third"));
        store.save(createServer(1L, "First"));
        store.save(createServer(2L, "Second"));

        List<ManagedServer> result = store.findAll();

        assertEquals(3, result.size());
        assertEquals("First", result.get(0).getName());
        assertEquals("Second", result.get(1).getName());
        assertEquals("Third", result.get(2).getName());
    }

    @Test
    void findById_withExistingId_returnsServer() {
        ManagedServer original = store.save(createServer(0L, "TestServer"));

        Optional<ManagedServer> found = store.findById(original.getId());

        assertTrue(found.isPresent());
        assertEquals(original.getId(), found.get().getId());
        assertEquals("TestServer", found.get().getName());
    }

    @Test
    void findById_withNonExistingId_returnsEmpty() {
        Optional<ManagedServer> found = store.findById(999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void save_updatesExistingServer() {
        ManagedServer server = store.save(createServer(0L, "Original"));
        long savedId = server.getId();

        ManagedServer replacement = new ManagedServer(
            savedId, "Updated", "localhost", 25565, "pass", 25575,
            ServerStatus.RUNNING, null, null, null, 1L, LocalDateTime.now(), LocalDateTime.now()
        );
        store.save(replacement);

        ManagedServer found = store.findById(savedId).orElseThrow();
        assertEquals("Updated", found.getName());
        assertEquals(ServerStatus.RUNNING, found.getStatus());
    }

    @Test
    void findAll_afterMultipleSaves_returnsAllServers() {
        store.save(createServer(0L, "Server1"));
        store.save(createServer(0L, "Server2"));
        store.save(createServer(0L, "Server3"));

        List<ManagedServer> result = store.findAll();

        assertEquals(3, result.size());
    }

    @Test
    void findById_withIdZero_returnsEmpty() {
        store.save(createServer(0L, "Test"));
        Optional<ManagedServer> found = store.findById(0L);
        assertTrue(found.isEmpty());
    }

    @Test
    void concurrentSaves_workCorrectly() throws InterruptedException {
        Thread[] threads = new Thread[10];
        ManagedServer[] servers = new ManagedServer[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                servers[index] = store.save(createServer(0L, "Server" + index));
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }

        assertEquals(10, store.findAll().size());
    }

    @Test
    void findAll_returnsLiveViewOfStore() {
        List<ManagedServer> view = store.findAll();
        assertTrue(view.isEmpty());

        store.save(createServer(0L, "NewServer"));
        assertEquals(1, store.findAll().size());
    }

    private ManagedServer createServer(long id, String name) {
        return new ManagedServer(
            id, name, "localhost", 25565, "pass", 25575,
            ServerStatus.STOPPED, null, null, null, 1L, LocalDateTime.now(), null
        );
    }
}
