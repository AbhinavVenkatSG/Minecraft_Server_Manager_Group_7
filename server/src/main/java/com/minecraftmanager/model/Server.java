package com.minecraftmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "servers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Server {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String host;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(nullable = false)
    private String rconPassword;
    
    private Integer rconPort;
    
    @Enumerated(EnumType.STRING)
    private ServerStatus status;
    
    @Column(length = 1000)
    private String serverProperties;
    
    private String backupPath;
    
    @Column(nullable = false)
    private Long ownerId;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastStarted;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ServerStatus.STOPPED;
        }
    }
    
    public enum ServerStatus {
        RUNNING,
        STOPPED,
        STARTING,
        STOPPING,
        ERROR
    }
}
