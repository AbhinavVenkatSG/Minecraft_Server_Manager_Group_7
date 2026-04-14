package app.auth.dto;

import domain.user.User;

public class LoginResponse {
    private final String token;
    private final long userId;
    private final String username;

    public LoginResponse(String token, long userId, String username) {
        this.token = token;
        this.userId = userId;
        this.username = username;
    }

    public static LoginResponse fromUser(User user) {
        return new LoginResponse(user.getApiToken(), user.getId(), user.getUsername());
    }

    public String getToken() {
        return token;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
