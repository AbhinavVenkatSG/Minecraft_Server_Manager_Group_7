package com.minecraftmanager.service;

import com.minecraftmanager.repository.ServerRepository;
import org.springframework.stereotype.Service;

@Service
public class ServerService {
    
    private final ServerRepository serverRepository;
    
    public ServerService(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }
}
