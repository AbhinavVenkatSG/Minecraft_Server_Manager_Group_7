package com.minecraftmanager.service;

import com.minecraftmanager.model.User;
import com.minecraftmanager.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public Optional<User> login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password));
    }
    
    public User register(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setApiToken(UUID.randomUUID().toString());
        return userRepository.save(user);
    }
    
    public Optional<User> validateToken(String token) {
        return userRepository.findByApiToken(token);
    }
}
