package app.auth;

import app.auth.dto.LoginRequest;
import app.auth.dto.LoginResponse;
import app.auth.dto.RegisterRequest;
import domain.user.User;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private final UserStore userStore;

    public AuthService(UserStore userStore) {
        this.userStore = userStore;
    }

    public Optional<LoginResponse> login(LoginRequest request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            return Optional.empty();
        }

        return userStore.findByUsername(request.getUsername().trim())
                .filter(user -> user.getPassword().equals(request.getPassword()))
                .map(LoginResponse::fromUser);
    }

    public Optional<LoginResponse> register(RegisterRequest request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            return Optional.empty();
        }

        String username = request.getUsername().trim();
        if (userStore.findByUsername(username).isPresent()) {
            return Optional.empty();
        }

        User user = new User(
                0L,
                username,
                request.getPassword(),
                UUID.randomUUID().toString(),
                LocalDateTime.now()
        );

        return Optional.of(LoginResponse.fromUser(userStore.save(user)));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
