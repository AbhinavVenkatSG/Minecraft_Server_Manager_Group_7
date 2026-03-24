package com.minecraftmanager.repository;

import com.minecraftmanager.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    // TODO: add query methods as needed
}
