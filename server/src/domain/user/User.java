package domain.user;

import java.time.LocalDateTime;

public class User {
    private long id;
    private final String username;
    private final String passwordHash;
    private final String passwordSalt;
    private final String apiToken;
    private final LocalDateTime createdAt;

    public User(long id, String username, String passwordHash, String passwordSalt, String apiToken, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.apiToken = apiToken;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getApiToken() {
        return apiToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
