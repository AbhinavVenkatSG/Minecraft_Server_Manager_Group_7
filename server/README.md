# Server

This backend is now a plain Java project with no Maven, no Spring Boot, and no database setup.

## Folder Layout

```text
server/
├── build.bat
├── run.bat
├── out/                  # compiled classes (generated)
└── src/
    ├── main/            # program entry point
    ├── app/             # services and use cases
    ├── domain/          # simple data classes
    ├── core/            # reusable low-level logic
    └── infra/           # HTTP server and storage details
```

## Build

```bat
cd server
build.bat
```

## Run

```bat
cd server
run.bat
```

If port `8080` is busy, you can choose another one:

```bat
run.bat 8090
```

## What Changed

- `src/main/java/...` and Maven-specific nesting were removed.
- Spring Boot, JPA, Lombok, and `application.properties` were removed.
- The backend now uses:
  - `com.sun.net.httpserver.HttpServer` for simple HTTP routes
  - in-memory storage classes for beginner-friendly behavior
  - plain Java classes for domain models and services

## Current Scope

The backend now focuses on being easy to understand and easy to build.
The old WebSocket transport was removed from the runnable server, but the packet and CRC logic was kept in `src/core` so you can reuse it later if you add WebSockets back.
