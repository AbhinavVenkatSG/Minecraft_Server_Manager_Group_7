import { useState } from "react";
import { Power, RotateCcw, Square, Circle } from "lucide-react";
import { toast } from "sonner";

type ServerStatus = "online" | "offline" | "starting";

export default function ServerHeader() {
  const [status, setStatus] = useState<ServerStatus>("online");

  const handleStart = () => {
    setStatus("starting");
    setTimeout(() => {
      setStatus("online");
      toast.success("Server started");
    }, 2000);
  };

  const handleStop = () => {
    setStatus("offline");
    toast.info("Server stopped");
  };

  const handleRestart = () => {
    setStatus("starting");
    toast.info("Restarting server...");
    setTimeout(() => {
      setStatus("online");
      toast.success("Server restarted");
    }, 3000);
  };

  const statusConfig = {
    online: { label: "Online", color: "text-primary", dotClass: "bg-primary animate-pulse-glow" },
    offline: { label: "Offline", color: "text-destructive", dotClass: "bg-destructive" },
    starting: { label: "Starting...", color: "text-warning", dotClass: "bg-warning animate-pulse-glow" },
  };

  const s = statusConfig[status];

  return (
    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
      <div>
        <div className="flex items-center gap-3">
          <h1 className="text-xl font-bold text-foreground tracking-tight">
            ⛏ My Minecraft Server
          </h1>
          <div className="flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-secondary border border-border">
            <span className={`w-2 h-2 rounded-full ${s.dotClass}`} />
            <span className={`text-xs font-mono font-medium ${s.color}`}>{s.label}</span>
          </div>
        </div>
        <p className="text-xs text-muted-foreground mt-1 font-mono">
          Paper 1.20.4 • play.myserver.com:25565 • 4 players
        </p>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={handleStart}
          disabled={status !== "offline"}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md bg-primary text-primary-foreground hover:opacity-90 disabled:opacity-30 transition-all"
        >
          <Power size={13} /> Start
        </button>
        <button
          onClick={handleRestart}
          disabled={status !== "online"}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md bg-secondary text-secondary-foreground border border-border hover:bg-muted disabled:opacity-30 transition-all"
        >
          <RotateCcw size={13} /> Restart
        </button>
        <button
          onClick={handleStop}
          disabled={status === "offline"}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md bg-destructive text-destructive-foreground hover:opacity-90 disabled:opacity-30 transition-all"
        >
          <Square size={13} /> Stop
        </button>
      </div>
    </div>
  );
}
