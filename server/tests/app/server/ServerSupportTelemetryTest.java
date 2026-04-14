package app.server;

import infra.persistence.InMemoryServerStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerSupportTelemetryTest {

    @Test
    void resolveTelemetryProcess_prefersLiveChildProcessOverLauncherShell() throws IOException, InterruptedException {
        ServerSupport support = new ServerSupport(new InMemoryServerStore(), new ServerRuntimeState());
        Process launcher = new ProcessBuilder("cmd.exe", "/c", "ping", "-n", "4", "127.0.0.1").start();

        try {
            ServerRuntimeState.RuntimeContext context = new ServerRuntimeState.RuntimeContext(launcher);
            Thread.sleep(200L);

            Optional<ProcessHandle> telemetryProcess = support.resolveTelemetryProcess(context);

            assertTrue(telemetryProcess.isPresent());
            assertNotEquals(launcher.pid(), telemetryProcess.get().pid());
        } finally {
            launcher.destroyForcibly();
            launcher.waitFor();
        }
    }

    @Test
    void readProcessCpuLoad_withNoTrackedProcess_returnsEmpty() {
        ServerSupport support = new ServerSupport(new InMemoryServerStore(), new ServerRuntimeState());
        ServerRuntimeState.RuntimeContext context = new ServerRuntimeState.RuntimeContext(null);

        OptionalDouble cpuLoad = support.readProcessCpuLoad(context);

        assertTrue(cpuLoad.isEmpty());
    }
}
