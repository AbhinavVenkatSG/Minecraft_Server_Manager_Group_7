/**
 * @file LoginResponse.java
 * @brief Data transfer object for successful authentication responses.
 * @{
 */

package app.auth.dto;

import domain.user.User;

/**
 * @class LoginResponse
 * @brief DTO returned after successful login or registration.
 */
public class LoginResponse {
    private final String token;
    private final long userId;
    private final String username;

    /**
     * @brief Constructs a login response.
     * @param token The API authentication token
     * @param userId The user ID
     * @param username The username
     */
    public LoginResponse(String token, long userId, String username) {
        this.token = token;
        this.userId = userId;
        this.username = username;
    }

    /**
     * @brief Creates a response from a User entity.
     * @param user The user entity
     * @return A new LoginResponse populated from the user
     */
    public static LoginResponse fromUser(User user) {
        return new LoginResponse(user.getApiToken(), user.getId(), user.getUsername());
    }

    /**
     * @return The API authentication token
     */
    public String getToken() {
        return token;
    }

    /**
     * @return The user ID
     */
    public long getUserId() {
        return userId;
    }

    /**
     * @return The username
     */
    public String getUsername() {
        return username;
    }
}
