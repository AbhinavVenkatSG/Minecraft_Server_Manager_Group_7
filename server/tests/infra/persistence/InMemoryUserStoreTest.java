package infra.persistence;

import domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserStoreTest {

    private InMemoryUserStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryUserStore();
    }

    @Test
    void save_newUser_assignsIdAndReturnsUser() {
        User user = createUser(0L, "testuser");
        User saved = store.save(user);

        assertEquals(1L, saved.getId());
        assertEquals("testuser", saved.getUsername());
    }

    @Test
    void save_multipleUsers_assignsSequentialIds() {
        User user1 = store.save(createUser(0L, "user1"));
        User user2 = store.save(createUser(0L, "user2"));
        User user3 = store.save(createUser(0L, "user3"));

        assertEquals(1L, user1.getId());
        assertEquals(2L, user2.getId());
        assertEquals(3L, user3.getId());
    }

    @Test
    void save_userWithExistingId_doesNotChangeId() {
        User user = store.save(createUser(99L, "testuser"));
        assertEquals(99L, user.getId());
    }

    @Test
    void findByUsername_withExistingUser_returnsUser() {
        User original = store.save(createUser(0L, "existinguser"));

        Optional<User> found = store.findByUsername("existinguser");

        assertTrue(found.isPresent());
        assertEquals(original.getId(), found.get().getId());
        assertEquals("existinguser", found.get().getUsername());
    }

    @Test
    void findByUsername_withNonExistingUser_returnsEmpty() {
        Optional<User> found = store.findByUsername("nonexistent");
        assertTrue(found.isEmpty());
    }

    @Test
    void findByUsername_isCaseSensitive() {
        store.save(createUser(0L, "TestUser"));

        assertTrue(store.findByUsername("TestUser").isPresent());
        assertTrue(store.findByUsername("testuser").isEmpty());
        assertTrue(store.findByUsername("TESTUSER").isEmpty());
    }

    @Test
    void save_duplicateUsername_overwritesPrevious() {
        User user1 = store.save(createUser(0L, "sameuser"));
        User user2 = store.save(createUser(0L, "sameuser"));

        assertEquals(1L, user1.getId());
        assertEquals(2L, user2.getId());

        Optional<User> found = store.findByUsername("sameuser");
        assertTrue(found.isPresent());
        assertEquals(2L, found.get().getId());
    }

    @Test
    void save_withSpecialCharactersInUsername_worksCorrectly() {
        User user = store.save(createUser(0L, "user@domain.com"));
        Optional<User> found = store.findByUsername("user@domain.com");

        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    void save_multipleUsers_concurrentIdGeneration() throws InterruptedException {
        Thread[] threads = new Thread[10];
        User[] users = new User[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                users[index] = store.save(createUser(0L, "user" + index));
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }

        for (User user : users) {
            assertNotNull(user);
            assertTrue(user.getId() > 0);
        }
    }

    private User createUser(long id, String username) {
        return new User(id, username, "hash", "salt", "token", LocalDateTime.now());
    }
}
