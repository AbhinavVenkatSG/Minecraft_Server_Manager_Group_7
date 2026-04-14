package app.auth;

import domain.user.User;

import java.util.Optional;

public interface UserStore {
    Optional<User> findByUsername(String username);

    User save(User user);
}
