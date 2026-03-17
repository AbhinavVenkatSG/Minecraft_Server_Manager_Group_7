package com.minecraftmanager.service;

import com.minecraftmanager.model.Backup;
import com.minecraftmanager.repository.BackupRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BackupService {
    
    private final BackupRepository backupRepository;
    
    public BackupService(BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }
    
    public List<Backup> getBackupsByServer(Long serverId) {
        return backupRepository.findByServerIdOrderByCreatedAtDesc(serverId);
    }
    
    public Backup createBackup(Backup backup) {
        return backupRepository.save(backup);
    }
    
    public Optional<Backup> getBackup(Long id) {
        return backupRepository.findById(id);
    }
    
    public void deleteBackup(Long id) {
        backupRepository.deleteById(id);
    }
}
