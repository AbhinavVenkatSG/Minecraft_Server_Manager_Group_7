package test;

import app.auth.AuthService;
import app.auth.dto.LoginRequest;
import app.auth.dto.RegisterRequest;
import infra.persistence.FileUserStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AuthServiceTests {
    private AuthServiceTests() {
    }

    public static void registerAndLoginUsingEncryptedPersistence() throws Exception {
        Path root = Files.createTempDirectory("auth-service-tests-");
        try {
            AuthService authService = new AuthService(new FileUserStore(root));

            var registration = authService.register(new RegisterRequest("steve", "diamond-sword"));
            Assertions.assertTrue(registration.isPresent(), "REQ-AUTH-001: registration should create a client account.");

            var login = authService.login(new LoginRequest("steve", "diamond-sword"));
            Assertions.assertTrue(login.isPresent(), "REQ-AUTH-002: login should validate the stored credentials.");
            Assertions.assertEquals("steve", login.get().getUsername(), "Login should return the registered username.");

            Path userFile = root.resolve("users").resolve("1.dat");
            Assertions.assertTrue(Files.exists(userFile), "REQ-AUTH-001: registration should persist the account.");

            String encryptedPayload = new String(Files.readAllBytes(userFile), StandardCharsets.ISO_8859_1);
            Assertions.assertFalse(encryptedPayload.contains("steve"), "User file should not contain the username in plaintext.");
            Assertions.assertFalse(encryptedPayload.contains("diamond-sword"), "User file should not contain the password in plaintext.");
        } finally {
            deleteRecursively(root);
        }
    }

    private static void deleteRecursively(Path target) throws Exception {
        if (!Files.exists(target)) {
            return;
        }
        try (var paths = Files.walk(target)) {
            paths.sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        }
    }
}
