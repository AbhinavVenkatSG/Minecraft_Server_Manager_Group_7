import { useState } from "react";
import { Power, RotateCcw, Square, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useWebSocket } from "../contexts/WebSocketContext";

export default function ServerHeader({ server, onUpdate }) {
  const { startServer, stopServer, restartServer, servers } = useWebSocket();
  const [loading, setLoading] = useState(false);
  const [action, setAction] = useState(null);

  const currentServer = servers.find(s => s.id === server?.id) || server;
  const status = currentServer?.status?.toLowerCase() || "stopped";
  
  const isRunning = status === "running";
  const isStopped = status === "stopped";
  const isStarting = status === "startup" || status === "blocked";

  const handleStart = async () => {
    if (!server?.id) return;
    setLoading(true);
    setAction("start");
    try {
      startServer(server.id);
      toast.success("Server starting...");
      if (onUpdate) onUpdate();
    } catch (error) {
      toast.error(`Failed to start server: ${error.message}`);
    } finally {
      setLoading(false);
      setAction(null);
    }
  };

  const handleStop = async () => {
    if (!server?.id) return;
    setLoading(true);
    setAction("stop");
    try {
      stopServer(server.id);
      toast.info("Server stopping...");
      if (onUpdate) onUpdate();
    } catch (error) {
      toast.error(`Failed to stop server: ${error.message}`);
    } finally {
      setLoading(false);
      setAction(null);
    }
  };

  const handleRestart = async () => {
    if (!server?.id) return;
    setLoading(true);
    setAction("restart");
    try {
      restartServer(server.id);
      toast.info("Server restarting...");
      if (onUpdate) onUpdate();
    } catch (error) {
      toast.error(`Failed to restart server: ${error.message}`);
    } finally {
      setLoading(false);
      setAction(null);
    }
  };

  const statusConfig = {
    running: { label: "Online", color: "text-primary", dotClass: "bg-primary animate-pulse-glow" },
    stopped: { label: "Offline", color: "text-destructive", dotClass: "bg-destructive" },
    startup: { label: "Starting...", color: "text-warning", dotClass: "bg-warning animate-pulse-glow" },
    blocked: { label: "Blocked", color: "text-destructive", dotClass: "bg-destructive animate-pulse-glow" },
  };

  const s = statusConfig[status] || statusConfig.stopped;

  return (
    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
      <div>
        <div className="flex items-center gap-3">
          <h1 className="text-xl font-bold text-foreground tracking-tight">
            {server?.name || "No Server Selected"}
          </h1>
          <div className="flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-secondary border border-border">
            <span className={`w-2 h-2 rounded-full ${s.dotClass}`} />
            <span className={`text-xs font-mono font-medium ${s.color}`}>{s.label}</span>
          </div>
        </div>
        <p className="text-xs text-muted-foreground mt-1 font-mono">
          {server ? `${currentServer.host}:${currentServer.port}` : "Select a server"} - 
          {isRunning ? " Server running" : " Server stopped"}
        </p>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={handleStart}
          disabled={!server || !isStopped || loading}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md bg-primary text-primary-foreground hover:opacity-90 disabled:opacity-30 transition-all"
        >
          {loading && action === "start" ? <Loader2 size={13} className="animate-spin" /> : <Power size={13} />}
          Start
        </button>
        <button
          onClick={handleRestart}
          disabled={!server || !isRunning || loading}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md bg-secondary text-secondary-foreground border border-border hover:bg-muted disabled:opacity-30 transition-all"
        >
          {loading && action === "restart" ? <Loader2 size={13} className="animate-spin" /> : <RotateCcw size={13} />}
          Restart
        </button>
        <button
          onClick={handleStop}
          disabled={!server || !isRunning || loading}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md bg-destructive text-destructive-foreground hover:opacity-90 disabled:opacity-30 transition-all"
        >
          {loading && action === "stop" ? <Loader2 size={13} className="animate-spin" /> : <Square size={13} />}
          Stop
        </button>
      </div>
    </div>
  );
}