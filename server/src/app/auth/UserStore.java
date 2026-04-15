/**
 * @file UserStore.java
 * @brief Repository interface for user persistence operations.
 * @{
 */

package app.auth;

import domain.user.User;

import java.util.Optional;

/**
 * @interface UserStore
 * @brief Abstract interface for user data persistence.
 * @details Implementations may store users in memory, files, or databases.
 */
public interface UserStore {
    /**
     * @brief Checks whether any users exist in the store.
     * @return true if no users are stored
     */
    boolean isEmpty();

    /**
     * @brief Retrieves a user by username.
     * @param username The username to search for
     * @return The user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * @brief Persists a user to the store.
     * @param user The user to save
     * @return The saved user with assigned ID
     */
    User save(User user);
}
