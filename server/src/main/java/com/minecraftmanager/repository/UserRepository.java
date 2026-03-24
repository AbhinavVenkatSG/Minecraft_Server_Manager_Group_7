package com.minecraftmanager.repository;

import com.minecraftmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // TODO: add query methods as needed
}
