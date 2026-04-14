import { useState, useEffect } from "react";
import { Cpu, HardDrive, MemoryStick, RefreshCw } from "lucide-react";
import { useWebSocket } from "../contexts/WebSocketContext";

function StatGauge({ label, value, max, icon, unit }) {
  const percentage = Math.min(Math.max(value || 0, 0), 100);
  const color =
    percentage > 85 ? "bg-destructive" : percentage > 60 ? "bg-warning" : "bg-primary";

  return (
    <div className="rounded-lg border border-border bg-card p-4 glow-green">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-primary">{icon}</span>
        <span className="text-sm font-medium text-foreground">{label}</span>
      </div>
      <div className="text-2xl font-bold font-mono text-foreground mb-1">
        {percentage}
        <span className="text-sm text-muted-foreground">{unit}</span>
      </div>
      <div className="text-xs text-muted-foreground mb-2">{max || "N/A"}</div>
      <div className="h-2 w-full rounded-full bg-secondary overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-700 ${color}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}

export default function ServerStats({ server, refreshKey }) {
  const { telemetry, subscribedServers, subscribeToServer, unsubscribeFromServer, requestTelemetry } = useWebSocket();
  const [loading, setLoading] = useState(false);

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
  const memoryUsage = serverTelemetry?.memoryUsage || 0;
  const diskUsage = serverTelemetry?.diskUsage || 0;

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <StatGauge
        label="CPU Usage"
        value={cpuUsage}
        max={serverTelemetry?.cpuInfo || "Server CPU"}
        icon={<Cpu size={18} />}
        unit="%"
      />
      <StatGauge
        label="Memory"
        value={memoryUsage}
        max={serverTelemetry?.memoryInfo || "Server Memory"}
        icon={<MemoryStick size={18} />}
        unit="%"
      />
      <StatGauge
        label="Storage"
        value={diskUsage}
        max={serverTelemetry?.diskInfo || "Server Storage"}
        icon={<HardDrive size={18} />}
        unit="%"
      />
    </div>
  );
}