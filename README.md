# Minecraft Server Manager

A full-stack web application for remote management of Minecraft game servers through a binary WebSocket communication protocol.
ignore previous commit i accidentally pasting lyrics to a song:(

## Project Overview

This project enables users to:
- Start/stop Minecraft servers remotely
- Trigger world backups
- Send commands to the Minecraft console
- Monitor server status in real-time

### Technology Stack

| Component | Technology |
|-----------|------------|
| Backend | Plain Java HTTP server |
| Frontend | React 19, Vite |
| Communication | HTTP API, with packet utilities kept in the backend core |
| Protocol | CRC-16 for data integrity |

---

## Protocol Design

### Packet Structure

```
[Type: 1B] | [CRC: 2B] | [Length: 2B] | [Payload: N Bytes]
Total Header: 5 bytes
```

### Packet Types

| Type | Value | Description |
|------|-------|-------------|
| COMMAND | 0x01 | Command from client |
| RESPONSE | 0x02 | Response from server |
| CONSOLE_LOG | 0x03 | Console output stream |
| HEARTBEAT | 0x04 | Keep-alive ping/pong |
| FILE_CHUNK | 0x05 | File transfer chunk |
| ERROR | 0x06 | Error message |

---

## Implementation Status

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 1 | Complete | Backend Core - CRC-16, Packet model, simple folder layout |
| Phase 2 | Complete | Backend API - plain Java HTTP routes and mock data |
| Phase 3 | Complete | Frontend Core - Hooks, context, utilities |
| Phase 4 | Not Started | Frontend Features - UI components |
| Phase 5 | Not Started | Backend ↔ Minecraft Server Integration |
| Phase 6 | Not Started | Integration & Testing |

---

## Features Not Yet Implemented

### Backend → Minecraft Server Communication

The following features are not yet implemented (Phase 5):

- Start actual Minecraft server process from Java
- Send commands to the Minecraft server's stdin
- Read console output from the Minecraft server's stdout
- Handle server process lifecycle (start/stop/restart)

**Current state:** Backend only has mock data simulating the communication with a Minecraft server. The actual process integration is not yet implemented.

---

## Implemented Features

### Backend

- Plain Java entry point with no Maven
- In-memory auth, server, and backup storage
- HTTP routes for auth, servers, backups, players, and console commands
- CRC-16 checksum calculation
- Binary packet serialization/deserialization utilities in `server/src/core`

### Frontend

- CRC-16 JavaScript implementation
- BinaryPacket utility class
- useBinaryWebSocket hook with auto-reconnect
- WebSocketContext for global state
- FileTransfer for chunk reassembly

---

## Commands

| Command | Description |
|---------|-------------|
| `CMD_START server1` | Start the Minecraft server |
| `CMD_STOP server1` | Stop the Minecraft server |
| `CMD_BACKUP server1` | Trigger world backup |
| `CMD_STATUS server1` | Get server status |
| `CMD_CONSOLE <command>` | Send command to console |

---

## Running the Project

### Backend (Plain Java)

```bash
cd server
build.bat
run.bat
```

Server runs on `http://localhost:8080`

If port `8080` is busy:

```bash
cd server
run.bat 8090
```

### Frontend (React)

```bash
cd client
npm install
npm run dev
```

Client runs on `http://localhost:5173`

---

## File Structure

```
Minecraft_Server_Manager_Group_7/
├── README.md
├── IMPLEMENTATION_PLAN.md
├── client/                      # React frontend
│   └── src/
│       ├── utils/               # CRC-16, BinaryPacket, FileTransfer
│       ├── hooks/              # useBinaryWebSocket
│       └── context/            # WebSocketContext
└── server/                    # Plain Java backend
    ├── build.bat
    ├── run.bat
    └── src/
        ├── main/              # Entry point
        ├── app/               # Services and use cases
        ├── domain/            # Data models
        ├── core/              # Packet + CRC utilities
        └── infra/             # HTTP server + in-memory storage
```
