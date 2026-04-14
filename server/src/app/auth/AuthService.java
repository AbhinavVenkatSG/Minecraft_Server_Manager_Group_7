package app.auth;

import app.auth.dto.LoginRequest;
import app.auth.dto.LoginResponse;
import app.auth.dto.RegisterRequest;
import core.util.CryptoSupport;
import domain.user.User;

import java.io.Console;
import java.time.LocalDateTime;
import java.util.Scanner;
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
                .filter(user -> user.getPasswordHash().equals(
                        CryptoSupport.hashPassword(request.getPassword(), user.getPasswordSalt())
                ))
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

        String salt = CryptoSupport.generateSalt();
        User user = new User(
                0L,
                username,
                CryptoSupport.hashPassword(request.getPassword(), salt),
                salt,
                UUID.randomUUID().toString(),
                LocalDateTime.now()
        );

        return Optional.of(LoginResponse.fromUser(userStore.save(user)));
    }

    public void ensureInteractiveAccount() {
        if (!userStore.isEmpty()) {
            return;
        }

        Console console = System.console();
        Scanner scanner = console == null ? new Scanner(System.in) : null;

        String username;
        String password;
        do {
            username = readLine(console, scanner, "Create initial client username: ");
        } while (isBlank(username));

        do {
            password = readPassword(console, scanner, "Create initial client password: ");
        } while (isBlank(password));

        register(new RegisterRequest(username.trim(), password));
        System.out.println("Initial client account created for user '" + username.trim() + "'.");
    }

    private String readLine(Console console, Scanner scanner, String prompt) {
        return console != null ? console.readLine(prompt) : promptAndRead(scanner, prompt);
    }

    private String readPassword(Console console, Scanner scanner, String prompt) {
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            return chars == null ? "" : new String(chars);
        }
        return promptAndRead(scanner, prompt);
    }

    private String promptAndRead(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner != null && scanner.hasNextLine() ? scanner.nextLine() : "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
