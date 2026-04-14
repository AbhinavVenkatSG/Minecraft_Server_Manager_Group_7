package test;

import java.util.List;

public final class TestRunner {
    private TestRunner() {
    }

    public static void main(String[] args) {
        List<NamedTest> tests = List.of(
                new NamedTest("REQ-SVR-001 operational state normalization", ServerCatalogServiceTests::normalizesOperationalStateOnStartup),
                new NamedTest("REQ-SVR-101 Minecraft directory configuration persists", ServerCatalogServiceTests::persistsMinecraftDirectoryConfiguration),
                new NamedTest("REQ-SVR-201 server start", ServerLifecycleServiceTests::startsServerAndTracksRunningState),
                new NamedTest("REQ-SVR-202 start parameters read/write", ServerFileServiceTests::readsAndWritesStartParametersOnlyWhenStopped),
                new NamedTest("REQ-SVR-203 whitelist read/write", ServerFileServiceTests::readsAndWritesWhitelistOnlyWhenStopped),
                new NamedTest("REQ-SVR-204 world backup zip", BackupServiceTests::createsWorldBackupZipWhenServerIsStopped),
                new NamedTest("REQ-SVR-204 reject backups while running", BackupServiceTests::rejectsBackupCreationWhileServerIsRunning),
                new NamedTest("REQ-SVR-205 server.properties read/write", ServerFileServiceTests::readsAndWritesServerPropertiesOnlyWhenStopped),
                new NamedTest("REQ-SVR-301 stop and restart safely", ServerLifecycleServiceTests::sendsConsoleCommandsAndStopsAndRestartsSafely),
                new NamedTest("REQ-SVR-302/303/304/305/306/307 telemetry snapshot", ServerTelemetryServiceTests::collectsTelemetrySnapshotForServerRequirements),
                new NamedTest("REQ-AUTH register/login with encrypted persistence", AuthServiceTests::registerAndLoginUsingEncryptedPersistence)
        );

        int passed = 0;
        int failed = 0;

        for (NamedTest test : tests) {
            try {
                test.body().run();
                passed++;
                System.out.println("PASS " + test.name());
            } catch (Throwable throwable) {
                failed++;
                System.out.println("FAIL " + test.name());
                throwable.printStackTrace(System.out);
            }
        }

        System.out.println();
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);

        if (failed > 0) {
            System.exit(1);
        }
    }

    private record NamedTest(String name, Assertions.ThrowingRunnable body) {
    }
}
