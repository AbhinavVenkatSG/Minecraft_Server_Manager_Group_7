package infra.http;

import app.auth.AuthService;
import app.auth.dto.LoginRequest;
import app.auth.dto.LoginResponse;
import app.auth.dto.RegisterRequest;
import app.backup.BackupService;
import app.server.ServerService;
import app.server.dto.ServerRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import domain.backup.Backup;
import domain.server.ManagedServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

public class ApiServer {
    private final HttpServer httpServer;
    private final AuthService authService;
    private final ServerService serverService;
    private final BackupService backupService;

    public ApiServer(int port, AuthService authService, ServerService serverService, BackupService backupService) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.authService = authService;
        this.serverService = serverService;
        this.backupService = backupService;
        this.httpServer.createContext("/api", new ApiHandler());
        this.httpServer.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        httpServer.start();
    }

    private final class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 204, "");
                return;
            }

            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            String body = readBody(exchange);
            String[] parts = path.substring(1).split("/");

            try {
                if (parts.length >= 3 && "api".equals(parts[0]) && "auth".equals(parts[1])) {
                    handleAuth(exchange, method, parts, body);
                    return;
                }

                if (parts.length >= 2 && "api".equals(parts[0]) && "servers".equals(parts[1])) {
                    handleServers(exchange, method, parts, body);
                    return;
                }

                send(exchange, 404, Responses.message("Route not found."));
            } catch (Exception exception) {
                send(exchange, 500, Responses.message("Server error: " + exception.getMessage()));
            }
        }
    }

    private void handleAuth(HttpExchange exchange, String method, String[] parts, String body) throws IOException {
        if ("POST".equals(method) && parts.length == 3 && "login".equals(parts[2])) {
            Optional<LoginResponse> response = authService.login(new LoginRequest(
                    Json.readString(body, "username"),
                    Json.readString(body, "password")
            ));
            if (response.isPresent()) {
                send(exchange, 200, Responses.login(response.get()));
            } else {
                send(exchange, 401, Responses.message("Invalid username or password."));
            }
            return;
        }

        if ("POST".equals(method) && parts.length == 3 && "register".equals(parts[2])) {
            Optional<LoginResponse> response = authService.register(new RegisterRequest(
                    Json.readString(body, "username"),
                    Json.readString(body, "password")
            ));
            if (response.isPresent()) {
                send(exchange, 200, Responses.login(response.get()));
            } else {
                send(exchange, 400, Responses.message("Could not register user."));
            }
            return;
        }

        send(exchange, 404, Responses.message("Auth route not found."));
    }

    private void handleServers(HttpExchange exchange, String method, String[] parts, String body) throws IOException {
        if ("GET".equals(method) && parts.length == 2) {
            send(exchange, 200, Responses.servers(serverService.listServers()));
            return;
        }

        if ("POST".equals(method) && parts.length == 2) {
            ServerRequest request = new ServerRequest(
                    Json.readString(body, "name"),
                    Json.readString(body, "host"),
                    Json.readInt(body, "port", 25565),
                    Json.readString(body, "rconPassword"),
                    Json.readInt(body, "rconPort", 25575),
                    Json.readString(body, "serverProperties"),
                    Json.readString(body, "backupPath")
            );

            Optional<ManagedServer> createdServer = serverService.createServer(request, 1L);
            if (createdServer.isPresent()) {
                send(exchange, 201, Responses.server(createdServer.get()));
            } else {
                send(exchange, 400, Responses.message("Invalid server request."));
            }
            return;
        }

        if (parts.length < 3) {
            send(exchange, 404, Responses.message("Server route not found."));
            return;
        }

        long serverId = parseId(parts[2]);
        if (serverId < 0) {
            send(exchange, 400, Responses.message("Invalid server id."));
            return;
        }

        if ("GET".equals(method) && parts.length == 3) {
            Optional<ManagedServer> server = serverService.getServer(serverId);
            if (server.isPresent()) {
                send(exchange, 200, Responses.server(server.get()));
            } else {
                send(exchange, 404, Responses.message("Server not found."));
            }
            return;
        }

        if ("POST".equals(method) && parts.length == 4 && "start".equals(parts[3])) {
            Optional<ManagedServer> server = serverService.startServer(serverId);
            if (server.isPresent()) {
                send(exchange, 200, Responses.server(server.get()));
            } else {
                send(exchange, 404, Responses.message("Server not found."));
            }
            return;
        }

        if ("POST".equals(method) && parts.length == 4 && "stop".equals(parts[3])) {
            Optional<ManagedServer> server = serverService.stopServer(serverId);
            if (server.isPresent()) {
                send(exchange, 200, Responses.server(server.get()));
            } else {
                send(exchange, 404, Responses.message("Server not found."));
            }
            return;
        }

        if ("GET".equals(method) && parts.length == 4 && "backups".equals(parts[3])) {
            send(exchange, 200, Responses.backups(backupService.listBackups(serverId)));
            return;
        }

        if ("POST".equals(method) && parts.length == 4 && "backups".equals(parts[3])) {
            Optional<Backup> backup = backupService.createBackup(serverId);
            if (backup.isPresent()) {
                send(exchange, 201, Responses.backup(backup.get()));
            } else {
                send(exchange, 404, Responses.message("Server not found."));
            }
            return;
        }

        if ("GET".equals(method) && parts.length == 4 && "players".equals(parts[3])) {
            Optional<ManagedServer> server = serverService.getServer(serverId);
            if (server.isEmpty()) {
                send(exchange, 404, Responses.message("Server not found."));
                return;
            }

            List<String> players = server.get().getStatus().name().equals("RUNNING")
                    ? List.of("Steve", "Alex")
                    : List.of();
            send(exchange, 200, Responses.players(players));
            return;
        }

        if ("POST".equals(method) && parts.length == 4 && "console".equals(parts[3])) {
            Optional<ManagedServer> server = serverService.getServer(serverId);
            if (server.isEmpty()) {
                send(exchange, 404, Responses.message("Server not found."));
                return;
            }

            String command = Json.readString(body, "command");
            send(exchange, 200, Responses.message("Console command received: " + command));
            return;
        }

        send(exchange, 404, Responses.message("Server route not found."));
    }

    private long parseId(String rawValue) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    }

    private void send(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}
