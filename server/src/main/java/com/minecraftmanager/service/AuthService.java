package com.minecraftmanager.service;

import com.minecraftmanager.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
