import { useCallback, useEffect, useState } from "react";
import { Archive, Download, Loader2, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import api from "../services/api";
import { Button } from "./ui/button";

/**
 * Lists backups for the selected server and exposes create/download actions.
 */
export default function ServerBackups({ server, refreshKey }) {
  const [backups, setBackups] = useState([]);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);

  const loadBackups = useCallback(async () => {
    if (!server?.id) {
      setBackups([]);
      return;
    }

    setLoading(true);
    try {
      const response = await api.getBackups(server.id);
      if (response.status !== 200 || !Array.isArray(response.data)) {
        throw new Error(response.data?.message || "Failed to load backups");
      }

      setBackups(
        [...response.data].sort((left, right) => {
          const leftTime = Date.parse(left.createdAt || "") || 0;
          const rightTime = Date.parse(right.createdAt || "") || 0;
          return rightTime - leftTime || right.id - left.id;
        }),
      );
    } catch (error) {
      toast.error(`Failed to load backups: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [server?.id]);

  useEffect(() => {
    loadBackups();
  }, [loadBackups, refreshKey]);

  const handleCreateBackup = async () => {
    if (!server?.id) {
      return;
    }

    setCreating(true);
    try {
      const response = await api.createBackup(server.id);
      if (response.status !== 201 || !response.data?.id) {
        throw new Error(response.data?.message || "Failed to create backup");
      }

      toast.success(`Backup created: ${response.data.filename}`);
      await loadBackups();
    } catch (error) {
      toast.error(`Backup failed: ${error.message}`);
    } finally {
      setCreating(false);
    }
  };

  /**
   * Streams a backup blob into a temporary anchor download and cleans up the object URL.
   */
  const handleDownloadBackup = async (backup) => {
    setDownloadingId(backup.id);
    try {
      const { blob, filename } = await api.downloadBackup(backup.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = filename || backup.filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      toast.error(`Download failed: ${error.message}`);
    } finally {
      setDownloadingId(null);
    }
  };

  if (!server) {
    return (
      <div className="mt-4 rounded-lg border border-border bg-card p-4">
        <div className="text-sm text-muted-foreground">Select a server to manage backups.</div>
      </div>
    );
  }

  const isStopped = server.status?.toLowerCase() === "stopped";

  return (
    <div className="mt-4 rounded-lg border border-border bg-card">
      <div className="flex flex-col gap-3 border-b border-border px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-sm font-semibold text-foreground">Backups</h3>
          <p className="text-xs text-muted-foreground">
            Create a zip backup when the server is stopped, then download it from here.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={loadBackups}
            disabled={loading || creating}
          >
            {loading ? <Loader2 className="animate-spin" /> : <RefreshCw />}
            Refresh
          </Button>
          <Button
            size="sm"
            onClick={handleCreateBackup}
            disabled={!isStopped || creating}
            title={isStopped ? "Create backup" : "Stop the server before creating a backup"}
          >
            {creating ? <Loader2 className="animate-spin" /> : <Archive />}
            Create Backup
          </Button>
        </div>
      </div>

      <div className="divide-y divide-border">
        {backups.length === 0 ? (
          <div className="px-4 py-6 text-sm text-muted-foreground">
            {loading ? "Loading backups..." : "No backups have been created for this server yet."}
          </div>
        ) : (
          backups.map((backup) => (
            <div
              key={backup.id}
              className="flex flex-col gap-3 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <div className="truncate text-sm font-medium text-foreground">{backup.filename}</div>
                <div className="text-xs text-muted-foreground">
                  {formatBytes(backup.fileSize)} · {formatTimestamp(backup.createdAt)}
                </div>
              </div>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => handleDownloadBackup(backup)}
                disabled={downloadingId === backup.id}
              >
                {downloadingId === backup.id ? <Loader2 className="animate-spin" /> : <Download />}
                Download
              </Button>
            </div>
          ))
        )}
      </div>
    </div>
  );
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

/**
 * Formats an API timestamp into the user's local date/time string.
 */
function formatTimestamp(value) {
  if (!value) {
    return "Unknown time";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString();
}
