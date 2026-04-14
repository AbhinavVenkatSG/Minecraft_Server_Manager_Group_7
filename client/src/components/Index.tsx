import { useState, useEffect, useCallback } from "react";
import { Loader2, Plus, Server, LogOut } from "lucide-react";
import ServerHeader from "./ServerHeader";
import ServerStats from "./ServerStats";
import ChatLogs from "./ChatLogs";
import ConfigEditor from "./ConfigEditor";
import { useWebSocket } from "../contexts/WebSocketContext";
import { useAuth } from "../contexts/AuthContext";
import { Button } from "./ui/button";

export default function Index() {
  const { servers, isConnected, requestServerList } = useWebSocket();
  const { user, logout } = useAuth();
  const [selectedServer, setSelectedServer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    if (isConnected && servers.length > 0) {
      setLoading(false);
      if (servers.length > 0 && !selectedServer) {
        setSelectedServer(servers[0]);
      }
    }
  }, [isConnected, servers, selectedServer]);

  useEffect(() => {
    if (isConnected && servers.length === 0) {
      const timer = setTimeout(() => {
        setLoading(false);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [isConnected, servers.length]);

  const handleServerUpdate = () => {
    setRefreshKey((k) => k + 1);
    requestServerList();
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4">
          <Loader2 size={48} className="animate-spin text-primary" />
          <p className="text-muted-foreground">Connecting to server...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen p-4 md:p-6 max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-foreground">Servers</h2>
        {user && (
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">{user.username}</span>
            <Button variant="ghost" size="sm" onClick={logout}>
              <LogOut size={14} />
            </Button>
          </div>
        )}
      </div>
      
      <div className="flex gap-2 flex-wrap mb-4">
        {servers.length === 0 ? (
          <p className="text-sm text-muted-foreground">No servers configured. Add one to get started.</p>
        ) : (
          servers.map((server) => (
            <button
              key={server.id}
              onClick={() => setSelectedServer(server)}
              className={`flex items-center gap-2 px-3 py-2 rounded-lg border text-sm transition-all ${
                selectedServer?.id === server.id
                  ? "bg-primary text-primary-foreground border-primary"
                  : "bg-card text-foreground border-border hover:border-primary"
              }`}
            >
              <Server size={14} />
              {server.name}
            </button>
          ))
        )}
      </div>

      <ServerHeader server={selectedServer} onUpdate={handleServerUpdate} />
      <ServerStats server={selectedServer} refreshKey={refreshKey} />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4" style={{ height: "calc(100vh - 320px)", minHeight: "400px" }}>
        <ChatLogs server={selectedServer} refreshKey={refreshKey} />
        <ConfigEditor server={selectedServer} refreshKey={refreshKey} />
      </div>
    </div>
  );
}