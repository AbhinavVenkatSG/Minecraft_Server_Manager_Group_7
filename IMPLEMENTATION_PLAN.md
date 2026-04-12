# Final Implementation Plan: Binary WebSocket Communication

## 1. Protocol Design

### Packet Structure
```
[Type: 1B] | [CRC: 2B] | [Length: 2B] | [Payload: N Bytes]
```
- **Total Header:** 5 bytes
- **CRC-16:** Validates packet integrity

### Command Types (Client → Server)
| Prefix | Purpose |
|--------|---------|
| `CMD_START` | Start Minecraft server |
| `CMD_STOP` | Stop Minecraft server |
| `CMD_BACKUP` | Trigger world backup |
| `CMD_CONSOLE` | Send command to Minecraft console |
| `CMD_STATUS` | Get server status |

### Response Types (Server → Client)
| Type | Value | Purpose |
|------|-------|---------|
| `COMMAND` | 0x01 | Command from client |
| `RESPONSE` | 0x02 | Command result |
| `CONSOLE_LOG` | 0x03 | Console output stream |
| `HEARTBEAT` | 0x04 | Keep-alive ping/pong |
| `FILE_CHUNK` | 0x05 | Backup file chunks |
| `ERROR` | 0x06 | Error messages |

---

## 2. File Structure

### Backend (Java/Spring Boot)
```
server/src/main/java/com/minecraftmanager/
├── websocket/
│   ├── config/
│   │   └── WebSocketConfig.java          # Enable binary messages
│   ├── handler/
│   │   └── BinaryPacketHandler.java      # Main WebSocket handler
│   ├── protocol/
│   │   ├── Packet.java                   # Packet model
│   │   ├── PacketType.java               # Enum of packet types
│   │   └── PacketBuilder.java            # Build packets
│   ├── util/
│   │   └── CRC16.java                    # CRC calculation
│   └── service/
│       └── FileTransferService.java      # Chunking & file handling
```

### Frontend (React)
```
client/src/
├── hooks/
│   └── useBinaryWebSocket.js             # WebSocket connection hook
├── context/
│   └── WebSocketContext.jsx              # Global WebSocket state
├── utils/
│   ├── crc16.js                          # CRC calculation
│   ├── BinaryPacket.js                   # Packet building/parsing
│   └── FileTransfer.js                   # Handle file chunks
└── components/
    └── ConsoleDisplay.jsx                # Display console logs
```

---

## 3. Implementation Phases

### Phase 1: Backend Core ✅ COMPLETE
- [x] CRC-16 utility (`websocket/util/CRC16.java`)
- [x] Packet model & builder (`websocket/protocol/Packet.java`, `PacketBuilder.java`)
- [x] Packet type enum (`websocket/protocol/PacketType.java`)
- [x] WebSocket config (binary enabled) (`websocket/config/WebSocketConfig.java`)
- [x] Binary packet handler (`websocket/handler/BinaryPacketHandler.java`)
- [x] API key validation (`websocket/config/ApiKeyHandshakeInterceptor.java`)

### Phase 2: Backend Protocol ✅ COMPLETE
- [x] Command parser (extract prefix & payload)
- [x] Response sender
- [x] Console log streaming handler
- [x] File chunk transfer service
- [x] Heartbeat handler

### Phase 3: Frontend Core ✅ COMPLETE
- [x] CRC-16 JS implementation (`client/src/utils/crc16.js`)
- [x] BinaryPacket utility class (`client/src/utils/BinaryPacket.js`)
- [x] useBinaryWebSocket hook (`client/src/hooks/useBinaryWebSocket.js`)
- [x] WebSocket context (`client/src/context/WebSocketContext.jsx`)

### Phase 4: Frontend Features
- [ ] File transfer handler (reassemble chunks)
- [ ] Console log display component
- [ ] Progress tracking

### Phase 5: Integration & Testing
- [ ] Connect frontend to backend
- [ ] Test all command types
- [ ] Test file transfer (large file)
- [ ] Heartbeat & reconnection

---

## 4. Key Details

### API Key Auth
- Passed via WebSocket query param: `ws://host/ws?apiKey=YOUR_KEY`
- Config stored in `application.properties`

### File Transfer
- One transfer at a time
- 64KB chunks
- File ID to track transfer
- Client triggers via `CMD_BACKUP` command

### Heartbeat
- Client sends every 30 seconds
- Server responds with same type

---

## 5. Example Flows

### Command Flow:
```
Client sends: [0x01, CRC, 7, "CMD_START server1"]
Server responds: [0x02, CRC, 12, "SUCCESS: Server started"]
```

### Console Log Flow:
```
Server sends: [0x03, CRC, 45, "[12:30:45] Player Notch joined"]
```

### Backup Flow:
```
Client sends: [0x01, CRC, 11, "CMD_BACKUP server1"]
Server sends: [0x02, CRC, 18, "BACKUP_STARTING..."]
Server sends: [0x05, CRC, chunk1] → chunk2 → ... → chunkN
Server sends: [0x02, CRC, 14, "BACKUP_COMPLETE"]
```

---

## 6. Implemented Files

### Backend (Phase 1)
| File | Path | Description |
|------|------|-------------|
| CRC16.java | `server/src/main/java/com/minecraftmanager/websocket/util/` | CRC-16 checksum calculation |
| PacketType.java | `server/src/main/java/com/minecraftmanager/websocket/protocol/` | Enum for packet types (COMMAND, RESPONSE, etc.) |
| Packet.java | `server/src/main/java/com/minecraftmanager/websocket/protocol/` | Packet model with toBytes/fromBytes |
| PacketBuilder.java | `server/src/main/java/com/minecraftmanager/websocket/protocol/` | Helper to build packets |
| WebSocketConfig.java | `server/src/main/java/com/minecraftmanager/websocket/config/` | WebSocket endpoint registration |
| ApiKeyHandshakeInterceptor.java | `server/src/main/java/com/minecraftmanager/websocket/config/` | API key validation on handshake |
| BinaryPacketHandler.java | `server/src/main/java/com/minecraftmanager/websocket/handler/` | Main WebSocket handler |

### Frontend (Phase 1 & 3)
| File | Path | Description |
|------|------|-------------|
| crc16.js | `client/src/utils/` | CRC-16 checksum (JS version) |
| BinaryPacket.js | `client/src/utils/` | Packet building/parsing utility |
| FileTransfer.js | `client/src/utils/` | Handle file chunk reassembly |
| useBinaryWebSocket.js | `client/src/hooks/` | WebSocket connection hook with auto-reconnect |
| WebSocketContext.jsx | `client/src/context/` | Global state provider for WebSocket |