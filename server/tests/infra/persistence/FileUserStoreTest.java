package infra.persistence;

import domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUserStoreTest {

    @Test
    void saveAndReload_preservesEncryptedUserData(@TempDir Path tempDir) throws Exception {
        FileUserStore store = new FileUserStore(tempDir);
        LocalDateTime createdAt = LocalDateTime.now().withNano(0);

        User saved = store.save(new User(
                0L,
                "testuser",
                "hashed-password",
                "salt",
                "token-123",
                createdAt
        ));

        byte[] persistedBytes = Files.readAllBytes(tempDir.resolve("users").resolve(saved.getId() + ".dat"));
        FileUserStore reloaded = new FileUserStore(tempDir);
        User restored = reloaded.findByUsername("testuser").orElseThrow();

        assertEquals("testuser", restored.getUsername());
        assertEquals("hashed-password", restored.getPasswordHash());
        assertEquals("token-123", restored.getApiToken());
        assertEquals(createdAt, restored.getCreatedAt());
        assertFalse(new String(persistedBytes, StandardCharsets.UTF_8).contains("testuser"));
        assertTrue(Files.exists(tempDir.resolve("security").resolve("users.key")));
    }

    @Test
    void reload_advancesGeneratedIds(@TempDir Path tempDir) {
        FileUserStore store = new FileUserStore(tempDir);
        store.save(new User(
                0L,
                "first",
                "hash1",
                "salt1",
                "token1",
                LocalDateTime.now().withNano(0)
        ));

        FileUserStore reloaded = new FileUserStore(tempDir);
        User second = reloaded.save(new User(
                0L,
                "second",
                "hash2",
                "salt2",
                "token2",
                LocalDateTime.now().withNano(0)
        ));

        assertEquals(2L, second.getId());
        assertTrue(reloaded.findByUsername("second").isPresent());
    }
}
