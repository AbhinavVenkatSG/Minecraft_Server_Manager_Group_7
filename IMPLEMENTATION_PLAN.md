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

### Phase 1: Backend Core
- [ ] CRC-16 utility
- [ ] Packet model & builder
- [ ] WebSocket config (binary enabled, 64KB max)
- [ ] Binary packet handler
- [ ] API key validation on handshake

### Phase 2: Backend Protocol
- [ ] Command parser (extract prefix & payload)
- [ ] Response sender
- [ ] Console log streaming handler
- [ ] File chunk transfer service
- [ ] Heartbeat handler

### Phase 3: Frontend Core
- [ ] CRC-16 JS implementation
- [ ] BinaryPacket utility class
- [ ] useBinaryWebSocket hook
- [ ] WebSocket context

### Phase 4: Frontend Features
- [ ] File transfer handler (reassemble chunks)
- [ ] Console log display
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