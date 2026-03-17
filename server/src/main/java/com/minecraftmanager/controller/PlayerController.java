package com.minecraftmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers/{serverId}/players")
public class PlayerController {
    
    @GetMapping
    public ResponseEntity<List<String>> getPlayers(@PathVariable Long serverId) {
        return ResponseEntity.ok(List.of());
    }
    
    @PostMapping("/{player}/kick")
    public ResponseEntity<Void> kickPlayer(@PathVariable Long serverId, @PathVariable String player) {
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{player}/ban")
    public ResponseEntity<Void> banPlayer(@PathVariable Long serverId, @PathVariable String player) {
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPlayerStats(@PathVariable Long serverId) {
        return ResponseEntity.ok(Map.of(
                "online", 0,
                "max", 20
        ));
    }
}
