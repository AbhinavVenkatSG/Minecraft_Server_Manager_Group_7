import { useState } from "react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "./ui/tabs";
import { Save, RotateCcw, FileText } from "lucide-react";
import { toast } from "sonner";

const MOCK_FILES = {
  "server.properties": `#Minecraft server properties
motd=\\u00A7a\\u00A7lMy Awesome Server
server-port=25565
gamemode=survival
difficulty=hard
max-players=20
online-mode=true
pvp=true
enable-command-block=false
spawn-protection=16
view-distance=12
simulation-distance=10
max-world-size=29999984
allow-flight=false
white-list=false
enforce-whitelist=false
spawn-npcs=true
spawn-animals=true
spawn-monsters=true
generate-structures=true
level-name=world
level-seed=
level-type=minecraft\\:normal
`,
  "spigot.yml": `settings:
  debug: false
  save-user-cache-on-stop-only: false
  bungeecord: false
  player-shuffle: 0
  netty-threads: 4
  timeout-time: 60
  restart-on-crash: true
  restart-script: ./start.sh
commands:
  replace-commands:
    - setblock
    - summon
    - testforblock
    - tellraw
  spam-exclusions:
    - /skill
  tab-complete: 0
`,
  "start.sh": `#!/bin/bash
# Minecraft Server Startup Script

JAVA_PATH="java"
MIN_RAM="4G"
MAX_RAM="12G"
JAR_FILE="paper-1.20.4.jar"

# JVM Flags (Aikar's flags for optimal GC)
JVM_FLAGS="-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1"

echo "Starting Minecraft Server..."
echo "RAM: \${MIN_RAM} - \${MAX_RAM}"

\${JAVA_PATH} -Xms\${MIN_RAM} -Xmx\${MAX_RAM} \${JVM_FLAGS} -jar \${JAR_FILE} nogui
`,
  "whitelist.json": `[
  {
    "uuid": "f7c77d99-9f15-4a66-a87d-c4a51ef30d19",
    "name": "Steve_42"
  },
  {
    "uuid": "a2080281-2345-4f6e-b890-c4d5e6f7a8b9",
    "name": "Alex_MC"
  },
  {
    "uuid": "b3090392-3456-5a7f-c901-d5e6f7a8b9c0",
    "name": "CreeperKing"
  }
]`,
};

export default function ConfigEditor() {
  const fileNames = Object.keys(MOCK_FILES);
  const [activeFile, setActiveFile] = useState(fileNames[0]);
  const [files, setFiles] = useState(MOCK_FILES);
  const [modified, setModified] = useState(new Set());

  const handleChange = (value) => {
    setFiles((prev) => ({ ...prev, [activeFile]: value }));
    setModified((prev) => new Set(prev).add(activeFile));
  };

  const handleSave = () => {
    setModified((prev) => {
      const next = new Set(prev);
      next.delete(activeFile);
      return next;
    });
    toast.success(`Saved ${activeFile}`);
  };

  const handleRevert = () => {
    setFiles((prev) => ({ ...prev, [activeFile]: MOCK_FILES[activeFile] }));
    setModified((prev) => {
      const next = new Set(prev);
      next.delete(activeFile);
      return next;
    });
    toast.info(`Reverted ${activeFile}`);
  };

  const lineCount = files[activeFile].split("\n").length;

  return (
    <div className="rounded-lg border border-border bg-card flex flex-col h-full">
      <Tabs value={activeFile} onValueChange={setActiveFile} className="flex flex-col h-full">
        <div className="flex items-center justify-between border-b border-border px-2">
          <TabsList className="bg-transparent h-auto p-0 gap-0">
            {fileNames.map((name) => (
              <TabsTrigger
                key={name}
                value={name}
                className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:shadow-none px-3 py-2.5 text-xs font-mono flex items-center gap-1.5"
              >
                <FileText size={12} />
                {name}
                {modified.has(name) && (
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
              disabled={!modified.has(activeFile)}
              className="p-1.5 rounded text-muted-foreground hover:text-primary disabled:opacity-30 transition-colors"
              title="Save file"
            >
              <Save size={14} />
            </button>
          </div>
        </div>

        {fileNames.map((name) => (
          <TabsContent key={name} value={name} className="flex-1 m-0 min-h-0">
            <div className="flex h-full">
              <div className="py-3 px-2 text-right select-none border-r border-border min-w-[3rem] overflow-hidden">
                {Array.from({ length: lineCount }, (_, i) => (
                  <div key={i} className="text-[11px] leading-5 text-muted-foreground font-mono">
                    {i + 1}
                  </div>
                ))}
              </div>
              <textarea
                value={files[name]}
                onChange={(e) => handleChange(e.target.value)}
                spellCheck={false}
                className="flex-1 bg-transparent p-3 text-xs font-mono text-foreground leading-5 resize-none focus:outline-none min-h-0 overflow-auto"
              />
            </div>
          </TabsContent>
        ))}
      </Tabs>
    </div>
  );
}
