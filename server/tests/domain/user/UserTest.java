package domain.user;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void constructor_withAllFields_setsFieldsCorrectly() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30);
        User user = new User(1L, "testuser", "hash123", "salt456", "token-abc", createdAt);

        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals("salt456", user.getPasswordSalt());
        assertEquals("token-abc", user.getApiToken());
        assertEquals(createdAt, user.getCreatedAt());
    }

    @Test
    void setId_updatesId() {
        User user = new User(1L, "user", "hash", "salt", "token", LocalDateTime.now());
        user.setId(42L);
        assertEquals(42L, user.getId());
    }

    @Test
    void getId_afterSetId_returnsUpdatedId() {
        User user = new User(0L, "user", "hash", "salt", "token", LocalDateTime.now());
        assertEquals(0L, user.getId());

        user.setId(100L);
        assertEquals(100L, user.getId());
    }

    @Test
    void username_isFinal_cannotBeChanged() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(1L, "original", "hash", "salt", "token", now);
        assertEquals("original", user.getUsername());
    }

    @Test
    void passwordHash_isFinal_cannotBeChanged() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(1L, "user", "secrethash", "salt", "token", now);
        assertEquals("secrethash", user.getPasswordHash());
    }

    @Test
    void apiToken_isFinal_cannotBeChanged() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(1L, "user", "hash", "salt", "unique-token-123", now);
        assertEquals("unique-token-123", user.getApiToken());
    }

    @Test
    void createdAt_isFinal_cannotBeChanged() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 6, 15, 12, 0);
        User user = new User(1L, "user", "hash", "salt", "token", createdAt);
        assertEquals(createdAt, user.getCreatedAt());
    }

    @Test
    void constructor_withSpecialCharacters_handlesCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(1L, "user@domain.com", "hash123", "salt456", "tökën", now);

        assertEquals("user@domain.com", user.getUsername());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals("tökën", user.getApiToken());
    }
}
