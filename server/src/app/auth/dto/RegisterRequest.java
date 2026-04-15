/**
 * @file RegisterRequest.java
 * @brief Data transfer object for user registration requests.
 * @{
 */

package app.auth.dto;

/**
 * @class RegisterRequest
 * @brief DTO containing registration credentials.
 */
public class RegisterRequest {
    private final String username;
    private final String password;

    /**
     * @brief Constructs a register request.
     * @param username The desired username
     * @param password The desired password
     */
    public RegisterRequest(String username, String password) {
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
