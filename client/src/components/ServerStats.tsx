import { useEffect } from "react";
import { Cpu, HardDrive, MemoryStick } from "lucide-react";
import { useWebSocket } from "../contexts/WebSocketContext";

/**
 * Reusable stat tile used by the dashboard summary row.
 */
function StatCard({ label, value, detail, icon, unit, progress }) {
  const percentage = progress == null ? null : Math.min(Math.max(progress || 0, 0), 100);
  const color =
    percentage == null ? "bg-primary" : percentage > 85 ? "bg-destructive" : percentage > 60 ? "bg-warning" : "bg-primary";

  return (
    <div className="rounded-lg border border-border bg-card p-4 glow-green">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-primary">{icon}</span>
        <span className="text-sm font-medium text-foreground">{label}</span>
      </div>
      <div className="text-2xl font-bold font-mono text-foreground mb-1">
        {value}
        {unit ? <span className="text-sm text-muted-foreground">{unit}</span> : null}
      </div>
      <div className="text-xs text-muted-foreground mb-2">{detail || "N/A"}</div>
      {percentage != null ? (
        <div className="h-2 w-full rounded-full bg-secondary overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-700 ${color}`}
            style={{ width: `${percentage}%` }}
          />
        </div>
      ) : (
        <div className="h-2 w-full rounded-full bg-secondary/60" />
      )}
    </div>
  );
}

/**
 * Shows the latest CPU, memory, and storage figures for the selected server.
 */
export default function ServerStats({ server, refreshKey }) {
  const { telemetry, subscribedServers, subscribeToServer, requestTelemetry } = useWebSocket();

  useEffect(() => {
    if (server?.id) {
      if (!subscribedServers.has(server.id)) {
        subscribeToServer(server.id);
      }
    }
  }, [server?.id, subscribedServers, subscribeToServer]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (server?.id) {
        requestTelemetry(server.id);
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [server?.id, refreshKey, requestTelemetry]);

  const serverTelemetry = server ? telemetry[server.id] : null;

  if (!server) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="rounded-lg border border-border bg-card p-4 opacity-50">
          <div className="text-sm text-muted-foreground text-center">Select a server to view stats</div>
        </div>
        <div className="rounded-lg border border-border bg-card p-4 opacity-50" />
        <div className="rounded-lg border border-border bg-card p-4 opacity-50" />
      </div>
    );
  }

  const cpuUsage = serverTelemetry?.cpuUsage || 0;
  const memoryUsage = serverTelemetry?.memoryUsage ?? null;
  const memoryBytes = serverTelemetry?.memoryBytes || 0;
  const storageBytes = serverTelemetry?.storageBytes || 0;

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <StatCard
        label="CPU Usage"
        value={formatPercent(cpuUsage)}
        detail={serverTelemetry?.cpuInfo || "Minecraft server process"}
        icon={<Cpu size={18} />}
        unit="%"
        progress={cpuUsage}
      />
      <StatCard
        label="Memory"
        value={formatBytes(memoryBytes)}
        detail={serverTelemetry?.memoryInfo || "Minecraft server memory usage"}
        icon={<MemoryStick size={18} />}
        progress={memoryUsage}
      />
      <StatCard
        label="Storage"
        value={formatBytes(storageBytes)}
        detail={serverTelemetry?.storageInfo || "Minecraft server directory size"}
        icon={<HardDrive size={18} />}
      />
    </div>
  );
}

/**
 * Formats a percentage value for compact dashboard display.
 */
function formatPercent(value) {
  const rounded = Math.round((value || 0) * 10) / 10;
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1);
}

/**
 * Formats a byte count into a compact human-readable string.
 */
function formatBytes(value) {
  if (!value) {
    return "0 B";
  }

  const units = ["B", "KB", "MB", "GB", "TB"];
  let size = value;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }

  return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}
