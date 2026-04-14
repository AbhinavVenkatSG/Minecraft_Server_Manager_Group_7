package test;

import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerFileServiceTests {
    private ServerFileServiceTests() {
    }

    public static void readsAndWritesStartParametersOnlyWhenStopped() throws Exception {
        try (TestEnvironment environment = new TestEnvironment()) {
            String script = "@echo off\r\njava -Xms1G -Xmx2G -jar server.jar nogui\r\n";
            Path minecraftDirectory = environment.createMinecraftDirectory(script);
            ManagedServer server = environment.createServer(minecraftDirectory);

            String parameters = environment.fileService.readStartParameters(server.getId());
            Assertions.assertEquals("-Xms1G -Xmx2G", parameters, "REQ-SVR-202: start parameters should be readable.");

            environment.fileService.writeStartParameters(server.getId(), "-Xms2G -Xmx4G");
            String updatedScript = Files.readString(minecraftDirectory.resolve("start.bat"), StandardCharsets.UTF_8);
            Assertions.assertContains(updatedScript, "java -Xms2G -Xmx4G -jar server.jar nogui", "REQ-SVR-202: start parameters should be writable.");

            server.setStatus(ServerStatus.RUNNING);
            environment.getSupport().saveServer(server);
            Assertions.expectThrows(
                    IllegalStateException.class,
                    () -> environment.fileService.writeStartParameters(server.getId(), "-Xmx8G"),
                    "REQ-SVR-202: running servers must reject start-parameter edits."
            );
        }
    }

    public static void readsAndWritesWhitelistOnlyWhenStopped() throws Exception {
        try (TestEnvironment environment = new TestEnvironment()) {
            Path minecraftDirectory = environment.createMinecraftDirectory();
            ManagedServer server = environment.createServer(minecraftDirectory);

            String whitelist = environment.fileService.readWhitelist(server.getId());
            Assertions.assertContains(whitelist, "Alex", "REQ-SVR-203: whitelist contents should be readable.");

            environment.fileService.writeWhitelist(server.getId(), "[\"Steve\"]");
            String updatedWhitelist = Files.readString(minecraftDirectory.resolve("whitelist.json"), StandardCharsets.UTF_8);
            Assertions.assertEquals("[\"Steve\"]", updatedWhitelist, "REQ-SVR-203: whitelist contents should be writable.");

            server.setStatus(ServerStatus.RUNNING);
            environment.getSupport().saveServer(server);
            Assertions.expectThrows(
                    IllegalStateException.class,
                    () -> environment.fileService.writeWhitelist(server.getId(), "[]"),
                    "REQ-SVR-203: running servers must reject whitelist edits."
            );
        }
    }

    public static void readsAndWritesServerPropertiesOnlyWhenStopped() throws Exception {
        try (TestEnvironment environment = new TestEnvironment()) {
            Path minecraftDirectory = environment.createMinecraftDirectory();
            ManagedServer server = environment.createServer(minecraftDirectory);

            String properties = environment.fileService.readServerProperties(server.getId());
            Assertions.assertContains(properties, "motd=Test Server", "REQ-SVR-205: server.properties should be readable.");

            environment.fileService.writeServerProperties(server.getId(), "difficulty=hard");
            String updatedProperties = Files.readString(minecraftDirectory.resolve("server.properties"), StandardCharsets.UTF_8);
            Assertions.assertEquals("difficulty=hard", updatedProperties, "REQ-SVR-205: server.properties should be writable.");

            server.setStatus(ServerStatus.RUNNING);
            environment.getSupport().saveServer(server);
            Assertions.expectThrows(
                    IllegalStateException.class,
                    () -> environment.fileService.writeServerProperties(server.getId(), "pvp=false"),
                    "REQ-SVR-205: running servers must reject server.properties edits."
            );
        }
    }
}
