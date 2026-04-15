/**
 * @file LoginRequest.java
 * @brief Data transfer object for login requests.
 * @{
 */

package app.auth.dto;

/**
 * @class LoginRequest
 * @brief DTO containing login credentials.
 */
public class LoginRequest {
    private final String username;
    private final String password;

    /**
     * @brief Constructs a login request.
     * @param username The username
     * @param password The password
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return The password
     */
    public String getPassword() {
        return password;
    }
}
