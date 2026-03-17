package com.minecraftmanager.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerRequest {
    private String name;
    private String host;
    private Integer port;
    private String rconPassword;
    private Integer rconPort;
    private String serverProperties;
    private String backupPath;
}
