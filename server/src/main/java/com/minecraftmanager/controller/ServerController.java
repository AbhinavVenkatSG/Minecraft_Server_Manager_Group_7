package com.minecraftmanager.controller;

import com.minecraftmanager.dto.ServerRequest;
import com.minecraftmanager.model.Server;
import com.minecraftmanager.service.ServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
public class ServerController {
    
    private final ServerService serverService;
    
    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }
    
    @GetMapping
    public List<Server> getAllServers(@RequestHeader("X-API-Token") String token) {
        return serverService.getServersByOwner(1L);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Server> getServer(@PathVariable Long id) {
        return serverService.getServer(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public Server createServer(@RequestBody ServerRequest request) {
        Server server = new Server();
        server.setName(request.getName());
        server.setHost(request.getHost());
        server.setPort(request.getPort());
        server.setRconPassword(request.getRconPassword());
        server.setRconPort(request.getRconPort());
        server.setServerProperties(request.getServerProperties());
        server.setBackupPath(request.getBackupPath());
        server.setOwnerId(1L);
        return serverService.createServer(server);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Server> updateServer(@PathVariable Long id, @RequestBody ServerRequest request) {
        Server server = new Server();
        server.setName(request.getName());
        server.setHost(request.getHost());
        server.setPort(request.getPort());
        server.setRconPassword(request.getRconPassword());
        server.setRconPort(request.getRconPort());
        server.setServerProperties(request.getServerProperties());
        server.setBackupPath(request.getBackupPath());
        return serverService.updateServer(id, server)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/start")
    public ResponseEntity<Server> startServer(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/stop")
    public ResponseEntity<Server> stopServer(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/restart")
    public ResponseEntity<Server> restartServer(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/status")
    public ResponseEntity<String> getServerStatus(@PathVariable Long id) {
        return ResponseEntity.ok("STOPPED");
    }
}
