package test;

import domain.server.ManagedServer;
import domain.server.ServerStatus;

import java.nio.file.Path;

public final class ServerLifecycleServiceTests {
    private ServerLifecycleServiceTests() {
    }

    public static void startsServerAndTracksRunningState() throws Exception {
        try (TestEnvironment environment = new TestEnvironment()) {
            Path minecraftDirectory = environment.createMinecraftDirectory();
            ManagedServer server = environment.createServer(minecraftDirectory);

            var started = environment.startService.startServer(server.getId());
            Assertions.assertTrue(started.isPresent(), "REQ-SVR-201: start should succeed for configured servers.");
            Assertions.assertEquals(ServerStatus.RUNNING, started.get().getStatus(), "REQ-SVR-201: server status should become RUNNING.");
            Assertions.assertNotNull(started.get().getLastStarted(), "Starting a server should set lastStarted.");

            environment.waitForConsoleLine(server.getId(), "BOOTED");
            Assertions.assertTrue(
                    environment.getRuntimeState().getRuntimeContexts().containsKey(server.getId()),
                    "REQ-SVR-201: runtime context should be tracked for running servers."
            );

            environment.stopService.stopServer(server.getId());
        }
    }

    public static void sendsConsoleCommandsAndStopsAndRestartsSafely() throws Exception {
        try (TestEnvironment environment = new TestEnvironment()) {
            Path minecraftDirectory = environment.createMinecraftDirectory();
            ManagedServer server = environment.createServer(minecraftDirectory);

            environment.startService.startServer(server.getId());
            environment.waitForConsoleLine(server.getId(), "BOOTED");

            String response = environment.consoleService.sendConsoleCommand(server.getId(), "say hello");
            Assertions.assertContains(response, "say hello", "Console service should acknowledge sent commands.");
            environment.waitForConsoleLine(server.getId(), "CMD:say hello");

            var contextBeforeRestart = environment.getRuntimeState().getRuntimeContexts().get(server.getId());
            var restarted = environment.restartService.restartServer(server.getId());
            Assertions.assertTrue(restarted.isPresent(), "REQ-SVR-301: restart should succeed for running servers.");
            environment.waitForConsoleLine(server.getId(), "BOOTED");

            var contextAfterRestart = environment.getRuntimeState().getRuntimeContexts().get(server.getId());
            Assertions.assertTrue(contextBeforeRestart != contextAfterRestart, "REQ-SVR-301: restart should replace the process context.");
            Assertions.assertEquals(ServerStatus.RUNNING, restarted.get().getStatus(), "REQ-SVR-301: restart should return the server to RUNNING.");

            var stopped = environment.stopService.stopServer(server.getId());
            Assertions.assertTrue(stopped.isPresent(), "REQ-SVR-301: stop should succeed for running servers.");
            Assertions.assertEquals(ServerStatus.STOPPED, stopped.get().getStatus(), "REQ-SVR-301: stop should move the server to STOPPED.");
            Assertions.assertFalse(
                    environment.getRuntimeState().getRuntimeContexts().containsKey(server.getId()),
                    "REQ-SVR-301: runtime context should be cleared after stop."
            );
        }
    }
}
