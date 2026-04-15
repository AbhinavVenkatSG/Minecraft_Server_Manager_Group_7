import { useState, useRef, useEffect } from "react";
import { Send, RefreshCw, Loader2 } from "lucide-react";
import { useWebSocket } from "../contexts/WebSocketContext";

const typeColors = {
  info: "text-muted-foreground",
  warn: "text-warning",
  error: "text-destructive",
  chat: "text-foreground",
  join: "text-primary",
  leave: "text-destructive",
  command: "text-info",
};

/**
 * Console viewer and command box for the selected server.
 */
export default function ChatLogs({ server, refreshKey }) {
  const {
    consoleLogs,
    subscribedServers,
    subscribeToServer,
    unsubscribeFromServer,
    sendConsoleCommand,
    requestConsoleLogs,
    clearConsoleLogs,
  } = useWebSocket();
  const [command, setCommand] = useState("");
  const [sending, setSending] = useState(false);
  const scrollRef = useRef(null);

  const lines = server ? consoleLogs[server.id] || [] : [];

  useEffect(() => {
    if (server?.id) {
      if (!subscribedServers.has(server.id)) {
        subscribeToServer(server.id);
      }

      requestConsoleLogs(server.id);
    }

    return () => {
      if (server?.id) {
        unsubscribeFromServer(server.id);
      }
    };
  }, [requestConsoleLogs, server?.id, subscribedServers, subscribeToServer, unsubscribeFromServer]);

  useEffect(() => {
    if (!server?.id) {
      return undefined;
    }

    // Polling fills gaps for console output while the current websocket flow stays lightweight.
    const intervalId = setInterval(() => {
      requestConsoleLogs(server.id);
    }, 3000);

    return () => clearInterval(intervalId);
  }, [refreshKey, requestConsoleLogs, server?.id]);

  useEffect(() => {
    scrollRef.current?.scrollTo(0, scrollRef.current.scrollHeight);
  }, [lines]);

  const handleSend = () => {
    if (!command.trim() || !server?.id) return;
    setSending(true);
    sendConsoleCommand(server.id, command)
      .then((result) => {
        if (result.success) {
          setCommand("");
        } else {
          console.error("Failed to send command:", result.error);
        }
      })
      .finally(() => {
        setSending(false);
      });
  };

  /**
   * Applies lightweight styling hints based on common Minecraft log patterns.
   */
  const formatLine = (line, index) => {
    const timestamp = new Date().toLocaleTimeString();
    let type = "info";
    
    if (line.includes("joined the game")) type = "join";
    else if (line.includes("left the game")) type = "leave";
    else if (line.startsWith("[CONSOLE]")) type = "command";
    else if (line.toLowerCase().includes("error")) type = "error";
    else if (line.toLowerCase().includes("warn")) type = "warn";

    return (
      <div key={index} className={`${typeColors[type]} leading-relaxed`}>
        <span className="text-muted-foreground/50 mr-2">[{timestamp}]</span>
        {line}
      </div>
    );
  };

  if (!server) {
    return (
      <div className="flex flex-col h-full rounded-lg border border-border bg-card">
        <div className="px-4 py-3 border-b border-border">
          <h3 className="text-sm font-semibold text-foreground">Console</h3>
        </div>
        <div className="flex-1 flex items-center justify-center text-muted-foreground text-sm">
          Select a server to view console logs
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full rounded-lg border border-border bg-card">
      <div className="px-4 py-3 border-b border-border flex items-center justify-between">
        <div>
          <h3 className="text-sm font-semibold text-foreground">Console</h3>
        </div>
        <button
          onClick={() => clearConsoleLogs(server.id)}
          className="p-1 rounded hover:bg-secondary transition-colors"
          title="Clear logs"
        >
          <RefreshCw size={14} />
        </button>
      </div>

      <div ref={scrollRef} className="flex-1 overflow-y-auto p-3 space-y-0.5 font-mono text-xs min-h-0 bg-black/20">
        {lines.length === 0 ? (
          <div className="text-muted-foreground text-center py-8">
            No console output yet
          </div>
        ) : (
          lines.map((line, index) => formatLine(line, index))
        )}
      </div>

      <div className="p-3 border-t border-border">
        <div className="flex gap-2">
          <input
            value={command}
            onChange={(e) => setCommand(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSend()}
            placeholder="Send command (e.g., list, help, op player)..."
            disabled={!server || sending}
            className="flex-1 bg-secondary border border-border rounded-md px-3 py-2 text-xs font-mono text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring disabled:opacity-50"
          />
          <button
            onClick={handleSend}
            disabled={!command.trim() || !server || sending}
            className="bg-primary text-primary-foreground rounded-md px-3 py-2 hover:opacity-90 transition-opacity disabled:opacity-50"
          >
            {sending ? <Loader2 size={14} className="animate-spin" /> : <Send size={14} />}
          </button>
        </div>
      </div>
    </div>
  );
}
