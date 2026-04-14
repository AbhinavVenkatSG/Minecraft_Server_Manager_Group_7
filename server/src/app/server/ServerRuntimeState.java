package app.server;

import domain.server.TelemetrySnapshot;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public class ServerRuntimeState {
    private final ConcurrentHashMap<Long, RuntimeContext> runtimeContexts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, TelemetrySnapshot> latestTelemetry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Deque<String>> telemetryHistory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> latestLogOffsets = new ConcurrentHashMap<>();

    public ConcurrentHashMap<Long, RuntimeContext> getRuntimeContexts() {
        return runtimeContexts;
    }

    public ConcurrentHashMap<Long, TelemetrySnapshot> getLatestTelemetry() {
        return latestTelemetry;
    }

    public ConcurrentHashMap<Long, Deque<String>> getTelemetryHistory() {
        return telemetryHistory;
    }

    public ConcurrentHashMap<Long, Long> getLatestLogOffsets() {
        return latestLogOffsets;
    }

    public static final class RuntimeContext {
        private final Process process;
        private final BufferedWriter input;
        private final Deque<String> consoleLines = new ArrayDeque<>();
        private volatile long lastCpuSampleNanos;
        private volatile long lastCpuDurationNanos;
        private volatile boolean manuallyStopping;
        private volatile Thread outputThread;

        public RuntimeContext(Process process) {
            this.process = process;
            this.input = process == null
                    ? new BufferedWriter(Writer.nullWriter())
                    : new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        }

        public Process getProcess() {
            return process;
        }

        public BufferedWriter getInput() {
            return input;
        }

        public Deque<String> getConsoleLines() {
            return consoleLines;
        }

        public long getLastCpuSampleNanos() {
            return lastCpuSampleNanos;
        }

        public void setLastCpuSampleNanos(long lastCpuSampleNanos) {
            this.lastCpuSampleNanos = lastCpuSampleNanos;
        }

        public long getLastCpuDurationNanos() {
            return lastCpuDurationNanos;
        }

        public void setLastCpuDurationNanos(long lastCpuDurationNanos) {
            this.lastCpuDurationNanos = lastCpuDurationNanos;
        }

        public boolean isManuallyStopping() {
            return manuallyStopping;
        }

        public void setManuallyStopping(boolean manuallyStopping) {
            this.manuallyStopping = manuallyStopping;
        }

        public Thread getOutputThread() {
            return outputThread;
        }

        public void setOutputThread(Thread outputThread) {
            this.outputThread = outputThread;
        }
    }
}
