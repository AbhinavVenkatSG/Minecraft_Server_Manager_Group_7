package app.auth;

import app.auth.dto.LoginRequest;
import app.auth.dto.LoginResponse;
import app.auth.dto.RegisterRequest;
import domain.user.User;
import infra.persistence.InMemoryUserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private InMemoryUserStore userStore;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userStore = new InMemoryUserStore();
        authService = new AuthService(userStore);
    }

    @Test
    void login_withValidCredentials_returnsLoginResponse() {
        authService.register(new RegisterRequest("testuser", "password123"));
        LoginRequest request = new LoginRequest("testuser", "password123");

        Optional<LoginResponse> response = authService.login(request);

        assertTrue(response.isPresent());
        assertEquals("testuser", response.get().getUsername());
        assertNotNull(response.get().getToken());
        assertTrue(response.get().getUserId() > 0);
    }

    @Test
    void login_withInvalidPassword_returnsEmpty() {
        authService.register(new RegisterRequest("testuser", "password123"));
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        Optional<LoginResponse> response = authService.login(request);

        assertTrue(response.isEmpty());
    }

    @Test
    void login_withNonExistentUser_returnsEmpty() {
        LoginRequest request = new LoginRequest("nonexistent", "password");

        Optional<LoginResponse> response = authService.login(request);

        assertTrue(response.isEmpty());
    }

    @Test
    void login_withNullRequest_returnsEmpty() {
        Optional<LoginResponse> response = authService.login(null);
        assertTrue(response.isEmpty());
    }

    @Test
    void login_withBlankUsername_returnsEmpty() {
        LoginRequest request = new LoginRequest("", "password");
        assertTrue(authService.login(request).isEmpty());

        request = new LoginRequest("   ", "password");
        assertTrue(authService.login(request).isEmpty());

        request = new LoginRequest(null, "password");
        assertTrue(authService.login(request).isEmpty());
    }

    @Test
    void login_withBlankPassword_returnsEmpty() {
        LoginRequest request = new LoginRequest("testuser", "");
        assertTrue(authService.login(request).isEmpty());

        request = new LoginRequest("testuser", "   ");
        assertTrue(authService.login(request).isEmpty());

        request = new LoginRequest("testuser", null);
        assertTrue(authService.login(request).isEmpty());
    }

    @Test
    void login_usernameIsTrimmed() {
        authService.register(new RegisterRequest("testuser", "password123"));
        LoginRequest request = new LoginRequest("  testuser  ", "password123");

        Optional<LoginResponse> response = authService.login(request);

        assertTrue(response.isPresent());
        assertEquals("testuser", response.get().getUsername());
    }

    @Test
    void login_passwordIsNotTrimmed() {
        authService.register(new RegisterRequest("testuser", "password123"));
        LoginRequest request = new LoginRequest("testuser", "  password123  ");

        Optional<LoginResponse> response = authService.login(request);

        assertTrue(response.isEmpty());
    }

    @Test
    void register_withValidRequest_returnsLoginResponse() {
        RegisterRequest request = new RegisterRequest("newuser", "password123");

        Optional<LoginResponse> response = authService.register(request);

        assertTrue(response.isPresent());
        assertEquals("newuser", response.get().getUsername());
        assertNotNull(response.get().getToken());
        assertTrue(response.get().getUserId() > 0);
    }

    @Test
    void register_createsUserInStore() {
        RegisterRequest request = new RegisterRequest("newuser", "password123");

        authService.register(request);

        Optional<LoginResponse> loginResponse = authService.login(new LoginRequest("newuser", "password123"));
        assertTrue(loginResponse.isPresent());
    }

    @Test
    void register_withDuplicateUsername_returnsEmpty() {
        authService.register(new RegisterRequest("existinguser", "password123"));
        RegisterRequest request = new RegisterRequest("existinguser", "newpassword");

        Optional<LoginResponse> response = authService.register(request);

        assertTrue(response.isEmpty());
    }

    @Test
    void register_usernameIsCaseSensitive() {
        authService.register(new RegisterRequest("TestUser", "password123"));
        RegisterRequest request = new RegisterRequest("testuser", "password123");

        Optional<LoginResponse> response = authService.register(request);

        assertTrue(response.isPresent());
    }

    @Test
    void register_withNullRequest_returnsEmpty() {
        assertTrue(authService.register(null).isEmpty());
    }

    @Test
    void register_withBlankUsername_returnsEmpty() {
        assertTrue(authService.register(new RegisterRequest("", "password")).isEmpty());
        assertTrue(authService.register(new RegisterRequest("   ", "password")).isEmpty());
        assertTrue(authService.register(new RegisterRequest(null, "password")).isEmpty());
    }

    @Test
    void register_withBlankPassword_returnsEmpty() {
        assertTrue(authService.register(new RegisterRequest("user", "")).isEmpty());
        assertTrue(authService.register(new RegisterRequest("user", "   ")).isEmpty());
        assertTrue(authService.register(new RegisterRequest("user", null)).isEmpty());
    }

    @Test
    void register_generatesUniqueTokens() {
        Optional<LoginResponse> response1 = authService.register(new RegisterRequest("user1", "pass"));
        Optional<LoginResponse> response2 = authService.register(new RegisterRequest("user2", "pass"));

        assertNotEquals(response1.get().getToken(), response2.get().getToken());
    }

    @Test
    void login_afterRegister_canLoginSuccessfully() {
        authService.register(new RegisterRequest("testuser", "password123"));
        Optional<LoginResponse> response = authService.login(new LoginRequest("testuser", "password123"));

        assertTrue(response.isPresent());
        assertEquals("testuser", response.get().getUsername());
    }
}
