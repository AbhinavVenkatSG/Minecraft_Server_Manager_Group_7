package com.minecraftmanager.controller;

import com.minecraftmanager.model.Backup;
import com.minecraftmanager.service.BackupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/backups")
public class BackupController {
    
    private final BackupService backupService;
    
    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }
    
    @GetMapping
    public List<Backup> getBackups(@PathVariable Long serverId) {
        return backupService.getBackupsByServer(serverId);
    }
    
    @PostMapping
    public ResponseEntity<Backup> createBackup(@PathVariable Long serverId) {
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{backupId}/restore")
    public ResponseEntity<Void> restoreBackup(@PathVariable Long serverId, @PathVariable Long backupId) {
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{backupId}")
    public ResponseEntity<Void> deleteBackup(@PathVariable Long serverId, @PathVariable Long backupId) {
        backupService.deleteBackup(backupId);
        return ResponseEntity.noContent().build();
    }
}
