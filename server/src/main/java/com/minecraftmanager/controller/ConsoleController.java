package com.minecraftmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/servers/{serverId}/console")
public class ConsoleController {
    
    @GetMapping("/output")
    public ResponseEntity<Map<String, String>> getConsoleOutput(@PathVariable Long serverId) {
        return ResponseEntity.ok(Map.of("output", ""));
    }
    
    @PostMapping("/input")
    public ResponseEntity<Void> sendCommand(@PathVariable Long serverId, @RequestBody Map<String, String> request) {
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable Long serverId) {
        return ResponseEntity.ok(Map.of(
                "cpuUsage", 0.0,
                "ramUsage", 0.0,
                "tps", 20.0
        ));
    }
}
