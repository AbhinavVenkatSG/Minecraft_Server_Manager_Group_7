package com.minecraftmanager.service;

import com.minecraftmanager.model.Server;
import com.minecraftmanager.repository.ServerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServerService {
    
    private final ServerRepository serverRepository;
    
    public ServerService(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }
    
    public List<Server> getServersByOwner(Long ownerId) {
        return serverRepository.findByOwnerId(ownerId);
    }
    
    public Optional<Server> getServer(Long id) {
        return serverRepository.findById(id);
    }
    
    public Server createServer(Server server) {
        return serverRepository.save(server);
    }
    
    public Optional<Server> updateServer(Long id, Server server) {
        return serverRepository.findById(id).map(existing -> {
            existing.setName(server.getName());
            existing.setHost(server.getHost());
            existing.setPort(server.getPort());
            existing.setRconPassword(server.getRconPassword());
            existing.setRconPort(server.getRconPort());
            existing.setServerProperties(server.getServerProperties());
            existing.setBackupPath(server.getBackupPath());
            return serverRepository.save(existing);
        });
    }
    
    public void deleteServer(Long id) {
        serverRepository.deleteById(id);
    }
}
