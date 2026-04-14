import { useState, useRef, useEffect } from "react";
import { Send } from "lucide-react";

interface ChatMessage {
  timestamp: string;
  sender: string;
  message: string;
  type: "chat" | "system" | "join" | "leave" | "command";
}

const MOCK_MESSAGES: ChatMessage[] = [
  { timestamp: "14:32:01", sender: "SERVER", message: "Server started on port 25565", type: "system" },
  { timestamp: "14:32:45", sender: "Steve_42", message: "joined the game", type: "join" },
  { timestamp: "14:33:12", sender: "Alex_MC", message: "joined the game", type: "join" },
  { timestamp: "14:33:20", sender: "Steve_42", message: "Hey everyone! Ready to build?", type: "chat" },
  { timestamp: "14:33:28", sender: "Alex_MC", message: "Let's goooo 🏗️", type: "chat" },
  { timestamp: "14:34:01", sender: "SERVER", message: "Auto-save complete", type: "system" },
  { timestamp: "14:35:15", sender: "Steve_42", message: "Anyone have extra diamonds?", type: "chat" },
  { timestamp: "14:35:30", sender: "Notch_Fan", message: "joined the game", type: "join" },
  { timestamp: "14:35:42", sender: "Alex_MC", message: "I've got 12, tp to me", type: "chat" },
  { timestamp: "14:36:00", sender: "Notch_Fan", message: "/gamemode creative", type: "command" },
  { timestamp: "14:36:01", sender: "SERVER", message: "Notch_Fan: insufficient permissions", type: "system" },
  { timestamp: "14:36:55", sender: "Steve_42", message: "lmao nice try", type: "chat" },
  { timestamp: "14:37:10", sender: "CreeperKing", message: "joined the game", type: "join" },
  { timestamp: "14:37:30", sender: "CreeperKing", message: "sssssSSSSS 💥", type: "chat" },
  { timestamp: "14:38:00", sender: "Alex_MC", message: "left the game", type: "leave" },
];

const typeColors: Record<string, string> = {
  chat: "text-foreground",
  system: "text-warning",
  join: "text-primary",
  leave: "text-destructive",
  command: "text-info",
};

export default function ChatLogs() {
  const [messages] = useState(MOCK_MESSAGES);
  const [command, setCommand] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo(0, scrollRef.current.scrollHeight);
  }, [messages]);

  const handleSend = () => {
    if (!command.trim()) return;
    setCommand("");
  };

  return (
    <div className="flex flex-col h-full rounded-lg border border-border bg-card">
      <div className="px-4 py-3 border-b border-border">
        <h3 className="text-sm font-semibold text-foreground">Chat & Console</h3>
        <p className="text-xs text-muted-foreground">
          {4} players online
        </p>
      </div>

      <div ref={scrollRef} className="flex-1 overflow-y-auto p-3 space-y-0.5 font-mono text-xs min-h-0">
        {messages.map((msg, i) => (
          <div key={i} className={`${typeColors[msg.type]} leading-relaxed`}>
            <span className="text-muted-foreground">[{msg.timestamp}]</span>{" "}
            {msg.type === "join" || msg.type === "leave" ? (
              <span className="italic">
                {msg.sender} {msg.message}
              </span>
            ) : (
              <>
                <span className="font-semibold text-primary">&lt;{msg.sender}&gt;</span>{" "}
                {msg.message}
              </>
            )}
          </div>
        ))}
      </div>

      <div className="p-3 border-t border-border">
        <div className="flex gap-2">
          <input
            value={command}
            onChange={(e) => setCommand(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSend()}
            placeholder="Type a command or message..."
            className="flex-1 bg-secondary border border-border rounded-md px-3 py-2 text-xs font-mono text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
          />
          <button
            onClick={handleSend}
            className="bg-primary text-primary-foreground rounded-md px-3 py-2 hover:opacity-90 transition-opacity"
          >
            <Send size={14} />
          </button>
        </div>
      </div>
    </div>
  );
}
