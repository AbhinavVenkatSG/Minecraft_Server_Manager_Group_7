import { useState, useEffect } from "react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "./ui/tabs";
import { Save, RotateCcw, FileText, Loader2 } from "lucide-react";
import { toast } from "sonner";
import api from "../services/api";

const FILE_TYPES = [
  { id: "server-properties", name: "server.properties", fetchFn: (id) => api.getServerProperties(id), saveFn: (id, content) => api.updateServerProperties(id, content) },
  { id: "whitelist", name: "whitelist.json", fetchFn: (id) => api.getWhitelist(id), saveFn: (id, content) => api.updateWhitelist(id, content) },
  { id: "start-parameters", name: "start.sh", fetchFn: (id) => api.getStartParameters(id), saveFn: (id, content) => api.updateStartParameters(id, content) },
];

/**
 * Tabbed editor for the small set of server-managed configuration files exposed by the API.
 */
export default function ConfigEditor({ server, refreshKey }) {
  const [activeFile, setActiveFile] = useState(FILE_TYPES[0].id);
  const [files, setFiles] = useState({});
  const [originalFiles, setOriginalFiles] = useState({});
  const [modified, setModified] = useState(new Set());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (server?.id) {
      fetchAllFiles();
    } else {
      setFiles({});
      setOriginalFiles({});
      setModified(new Set());
    }
  }, [server?.id, refreshKey]);

  /**
   * Loads the editable server files in sequence so each tab always has a defined value.
   */
  const fetchAllFiles = async () => {
    setLoading(true);
    const fetchedFiles = {};
    const originalFetched = {};
    
    try {
      for (const fileType of FILE_TYPES) {
        try {
          const response = await fileType.fetchFn(server.id);
          if (response.status === 200) {
            const content = response.data?.content || "";
            fetchedFiles[fileType.id] = content;
            originalFetched[fileType.id] = content;
          }
        } catch (error) {
          console.error(`Failed to fetch ${fileType.name}:`, error);
          fetchedFiles[fileType.id] = "";
          originalFetched[fileType.id] = "";
        }
      }
      setFiles(fetchedFiles);
      setOriginalFiles(originalFetched);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (value) => {
    setFiles((prev) => ({ ...prev, [activeFile]: value }));
    setModified((prev) => new Set(prev).add(activeFile));
  };

  const handleSave = async () => {
    if (!server?.id) return;
    setSaving(true);
    const fileType = FILE_TYPES.find(f => f.id === activeFile);
    
    try {
      await fileType.saveFn(server.id, files[activeFile]);
      setOriginalFiles((prev) => ({ ...prev, [activeFile]: files[activeFile] }));
      setModified((prev) => {
        const next = new Set(prev);
        next.delete(activeFile);
        return next;
      });
      toast.success(`Saved ${fileType.name}`);
    } catch (error) {
      toast.error(`Failed to save ${fileType.name}: ${error.message}`);
    } finally {
      setSaving(false);
    }
  };

  const handleRevert = () => {
    setFiles((prev) => ({ ...prev, [activeFile]: originalFiles[activeFile] }));
    setModified((prev) => {
      const next = new Set(prev);
      next.delete(activeFile);
      return next;
    });
    toast.info(`Reverted changes`);
  };

  const currentContent = files[activeFile] || "";
  const lineCount = currentContent.split("\n").length;

  if (!server) {
    return (
      <div className="rounded-lg border border-border bg-card flex flex-col h-full">
        <div className="px-4 py-3 border-b border-border">
          <h3 className="text-sm font-semibold text-foreground">Config Editor</h3>
        </div>
        <div className="flex-1 flex items-center justify-center text-muted-foreground text-sm">
          Select a server to edit config files
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-border bg-card flex flex-col h-full">
      <Tabs value={activeFile} onValueChange={setActiveFile} className="flex flex-col h-full">
        <div className="flex items-center justify-between border-b border-border px-2">
          <TabsList className="bg-transparent h-auto p-0 gap-0">
            {FILE_TYPES.map((fileType) => (
              <TabsTrigger
                key={fileType.id}
                value={fileType.id}
                className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:shadow-none px-3 py-2.5 text-xs font-mono flex items-center gap-1.5"
              >
                <FileText size={12} />
                {fileType.name}
                {modified.has(fileType.id) && (
                  <span className="w-1.5 h-1.5 rounded-full bg-warning" />
                )}
              </TabsTrigger>
            ))}
          </TabsList>

          <div className="flex items-center gap-1 pr-2">
            <button
              onClick={handleRevert}
              disabled={!modified.has(activeFile)}
              className="p-1.5 rounded text-muted-foreground hover:text-foreground disabled:opacity-30 transition-colors"
              title="Revert changes"
            >
              <RotateCcw size={14} />
            </button>
            <button
              onClick={handleSave}
              disabled={!modified.has(activeFile) || saving}
              className="p-1.5 rounded text-muted-foreground hover:text-primary disabled:opacity-30 transition-colors"
              title="Save file"
            >
              {saving ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
            </button>
          </div>
        </div>

        {loading ? (
          <div className="flex-1 flex items-center justify-center">
            <Loader2 size={24} className="animate-spin text-muted-foreground" />
          </div>
        ) : (
          FILE_TYPES.map((fileType) => (
            <TabsContent key={fileType.id} value={fileType.id} className="flex-1 m-0 min-h-0">
              <div className="flex h-full">
                <div className="py-3 px-2 text-right select-none border-r border-border min-w-[3rem] overflow-hidden">
                  {Array.from({ length: lineCount }, (_, i) => (
                    <div key={i} className="text-[11px] leading-5 text-muted-foreground font-mono">
                      {i + 1}
                    </div>
                  ))}
                </div>
                <textarea
                  value={files[fileType.id] || ""}
                  onChange={(e) => handleChange(e.target.value)}
                  spellCheck={false}
                  className="flex-1 bg-transparent p-3 text-xs font-mono text-foreground leading-5 resize-none focus:outline-none min-h-0 overflow-auto"
                  placeholder={`No ${fileType.name} file content`}
                />
              </div>
            </TabsContent>
          ))
        )}
      </Tabs>
    </div>
  );
}
