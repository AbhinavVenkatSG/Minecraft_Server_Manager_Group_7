package com.minecraftmanager.repository;

import com.minecraftmanager.model.Backup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupRepository extends JpaRepository<Backup, Long> {
    // TODO: add query methods as needed
}
