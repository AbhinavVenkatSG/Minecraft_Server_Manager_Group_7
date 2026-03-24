package com.minecraftmanager.service;

import com.minecraftmanager.repository.BackupRepository;
import org.springframework.stereotype.Service;

@Service
public class BackupService {
    
    private final BackupRepository backupRepository;
    
    public BackupService(BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }
}
