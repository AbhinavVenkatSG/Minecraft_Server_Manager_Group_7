package infra.persistence;

import app.auth.UserStore;
import core.util.CryptoSupport;
import domain.user.User;

import javax.crypto.SecretKey;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FileUserStore implements UserStore {
    private final Path userDirectory;
    private final SecretKey secretKey;
    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1L);

    public FileUserStore(Path rootDirectory) {
        try {
            this.userDirectory = rootDirectory.resolve("users");
            Files.createDirectories(userDirectory);
            this.secretKey = CryptoSupport.loadOrCreateKey(rootDirectory.resolve("security").resolve("users.key"));
            loadUsers();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize user store.", exception);
        }
    }

    @Override
    public boolean isEmpty() {
        return usersById.isEmpty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    public synchronized User save(User user) {
        if (user.getId() == 0L) {
            user.setId(nextId.getAndIncrement());
        } else {
            nextId.set(Math.max(nextId.get(), user.getId() + 1));
        }

        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUsername(), user);
        writeUser(user);
        return user;
    }

    private void loadUsers() throws Exception {
        try (var paths = Files.list(userDirectory)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".dat"))
                    .forEach(this::readUserUnchecked);
        }
    }

    private void readUserUnchecked(Path path) {
        try {
            byte[] encrypted = Files.readAllBytes(path);
            String plaintext = CryptoSupport.decrypt(encrypted, secretKey);
            Properties properties = new Properties();
            properties.load(new StringReader(plaintext));

            User user = new User(
                    Long.parseLong(properties.getProperty("id", "0")),
                    properties.getProperty("username", ""),
                    properties.getProperty("passwordHash", ""),
                    properties.getProperty("passwordSalt", ""),
                    properties.getProperty("apiToken", ""),
                    LocalDateTime.parse(properties.getProperty("createdAt"))
            );

            usersById.put(user.getId(), user);
            usersByUsername.put(user.getUsername(), user);
            nextId.set(Math.max(nextId.get(), user.getId() + 1));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load user file: " + path, exception);
        }
    }

    private void writeUser(User user) {
        try {
            Properties properties = new Properties();
            properties.setProperty("id", Long.toString(user.getId()));
            properties.setProperty("username", user.getUsername());
            properties.setProperty("passwordHash", user.getPasswordHash());
            properties.setProperty("passwordSalt", user.getPasswordSalt());
            properties.setProperty("apiToken", user.getApiToken());
            properties.setProperty("createdAt", user.getCreatedAt().toString());

            StringWriter writer = new StringWriter();
            properties.store(writer, null);

            byte[] encrypted = CryptoSupport.encrypt(writer.toString(), secretKey);
            Files.write(userDirectory.resolve(user.getId() + ".dat"), encrypted);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist user.", exception);
        }
    }
}
