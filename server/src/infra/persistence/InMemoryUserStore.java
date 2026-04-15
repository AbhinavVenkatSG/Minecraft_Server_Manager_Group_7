/**
 * @file InMemoryUserStore.java
 * @brief In-memory implementation of UserStore for testing.
 * @ingroup infra
 * @{
 */

package infra.persistence;

import app.auth.UserStore;
import domain.user.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @class InMemoryUserStore
 * @brief Non-persistent UserStore implementation for unit testing.
 */
public class InMemoryUserStore implements UserStore {
    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();

    @Override
    public boolean isEmpty() {
        return usersById.isEmpty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    public User save(User user) {
        if (user.getId() == 0L) {
            user.setId(nextId.getAndIncrement());
        }

        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUsername(), user);
        return user;
    }
}
