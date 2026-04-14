package infra.http;

import app.auth.AuthService;
import app.backup.BackupService;
import app.server.*;
import app.server.dto.ServerRequest;
import infra.persistence.InMemoryBackupStore;
import infra.persistence.InMemoryServerStore;
import infra.persistence.InMemoryUserStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.*;

class ApiServerTest {

    private static HttpServer httpServer;
    private static HttpClient httpClient;
    private static InMemoryUserStore userStore;
    private static InMemoryServerStore serverStore;
    private static InMemoryBackupStore backupStore;
    private static String tempMcDir;
    private int testPort;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        if (httpServer != null) {
            httpServer.stop(0);
            Thread.sleep(100);
        }

        Path tempDir = Files.createTempDirectory("mc_test");
        tempMcDir = tempDir.resolve("server").toString();
        Files.createDirectories(Path.of(tempMcDir));
        Files.writeString(Path.of(tempMcDir).resolve("server.properties"), "server-port=25565\n");

        userStore = new InMemoryUserStore();
        serverStore = new InMemoryServerStore();
        backupStore = new InMemoryBackupStore();
        testPort = 18080 + (int)(Math.random() * 10000);

        ServerSupport serverSupport = new ServerSupport(serverStore, new ServerRuntimeState());
        ServerCatalogService serverCatalogService = new ServerCatalogService(serverSupport);
        StartServerService startServerService = new StartServerService(serverSupport);
        StopServerService stopServerService = new StopServerService(serverSupport);
        RestartServerService restartServerService = new RestartServerService(
            serverCatalogService, startServerService, stopServerService
        );
        ServerConsoleService serverConsoleService = new ServerConsoleService(serverSupport);
        ServerFileService serverFileService = new ServerFileService(serverSupport);
        ServerTelemetryService serverTelemetryService = new ServerTelemetryService(serverSupport);

        httpServer = HttpServer.create(new InetSocketAddress(testPort), 0);
        httpServer.createContext("/api", new TestApiHandler(
            new AuthService(userStore),
            serverCatalogService,
            startServerService,
            stopServerService,
            restartServerService,
            serverConsoleService,
            serverFileService,
            serverTelemetryService,
            new BackupService(backupStore, serverStore),
            tempMcDir
        ));
        httpServer.setExecutor(null);
        httpServer.start();

        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + testPort;

        Thread.sleep(100);
    }

    private String sendRequest(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .method(method, HttpRequest.BodyPublishers.ofString(body != null ? body : ""));

        if (body != null && !body.isEmpty()) {
            builder.header("Content-Type", "application/json");
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return response.statusCode() + ":" + response.body();
    }

    @Test
    void registerEndpoint_withValidRequest_returns200() throws Exception {
        String body = "{\"username\":\"testuser\",\"password\":\"password123\"}";
        String response = sendRequest("POST", "/api/auth/register", body);

        assertTrue(response.startsWith("200:"));
        assertTrue(response.contains("\"username\":\"testuser\""));
        assertTrue(response.contains("\"token\""));
    }

    @Test
    void registerEndpoint_withDuplicateUser_returns400() throws Exception {
        String body = "{\"username\":\"testuser\",\"password\":\"password123\"}";
        sendRequest("POST", "/api/auth/register", body);
        String response = sendRequest("POST", "/api/auth/register", body);

        assertTrue(response.startsWith("400:"));
    }

    @Test
    void loginEndpoint_withValidCredentials_returns200() throws Exception {
        sendRequest("POST", "/api/auth/register", "{\"username\":\"testuser\",\"password\":\"password123\"}");
        String response = sendRequest("POST", "/api/auth/login", "{\"username\":\"testuser\",\"password\":\"password123\"}");

        assertTrue(response.startsWith("200:"));
        assertTrue(response.contains("\"username\":\"testuser\""));
    }

    @Test
    void loginEndpoint_withInvalidPassword_returns401() throws Exception {
        sendRequest("POST", "/api/auth/register", "{\"username\":\"testuser\",\"password\":\"password123\"}");
        String response = sendRequest("POST", "/api/auth/login", "{\"username\":\"testuser\",\"password\":\"wrongpassword\"}");

        assertTrue(response.startsWith("401:"));
    }

    @Test
    void listServers_withNoServers_returnsEmptyArray() throws Exception {
        String response = sendRequest("GET", "/api/servers", null);

        assertTrue(response.startsWith("200:"));
        assertTrue(response.contains("[]"));
    }

    @Test
    void createServer_withValidRequest_returns201() throws Exception {
        String body = "{\"name\":\"MyServer\",\"host\":\"localhost\",\"port\":25565,\"minecraftDirectory\":\"" + tempMcDir + "\"}";
        String response = sendRequest("POST", "/api/servers", body);

        assertTrue(response.startsWith("201:"), "Expected 201 but got: " + response);
        assertTrue(response.contains("\"name\":\"MyServer\""));
        assertTrue(response.contains("\"status\":\"STOPPED\""));
    }

    @Test
    void getServer_withExistingId_returns200() throws Exception {
        String body = "{\"name\":\"MyServer\",\"host\":\"localhost\",\"port\":25565,\"minecraftDirectory\":\"" + tempMcDir + "\"}";
        sendRequest("POST", "/api/servers", body);
        String response = sendRequest("GET", "/api/servers/1", null);

        assertTrue(response.startsWith("200:"), "Expected 200 but got: " + response);
        assertTrue(response.contains("\"name\":\"MyServer\""));
    }

    @Test
    void getServer_withNonExistingId_returns404() throws Exception {
        String response = sendRequest("GET", "/api/servers/999", null);

        assertTrue(response.startsWith("404:"));
    }

    @Test
    void unknownRoute_returns404() throws Exception {
        String response = sendRequest("GET", "/api/unknown", null);

        assertTrue(response.startsWith("404:"));
    }

    static class TestApiHandler implements com.sun.net.httpserver.HttpHandler {
        private final AuthService authService;
        private final ServerCatalogService serverCatalogService;
        private final StartServerService startServerService;
        private final StopServerService stopServerService;
        private final RestartServerService restartServerService;
        private final ServerConsoleService serverConsoleService;
        private final ServerFileService serverFileService;
        private final ServerTelemetryService serverTelemetryService;
        private final BackupService backupService;
        private final String minecraftDirectory;

        TestApiHandler(
            AuthService authService,
            ServerCatalogService serverCatalogService,
            StartServerService startServerService,
            StopServerService stopServerService,
            RestartServerService restartServerService,
            ServerConsoleService serverConsoleService,
            ServerFileService serverFileService,
            ServerTelemetryService serverTelemetryService,
            BackupService backupService,
            String minecraftDirectory
        ) {
            this.authService = authService;
            this.serverCatalogService = serverCatalogService;
            this.startServerService = startServerService;
            this.stopServerService = stopServerService;
            this.restartServerService = restartServerService;
            this.serverConsoleService = serverConsoleService;
            this.serverFileService = serverFileService;
            this.serverTelemetryService = serverTelemetryService;
            this.backupService = backupService;
            this.minecraftDirectory = minecraftDirectory;
        }

        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
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

        private void handleAuth(com.sun.net.httpserver.HttpExchange exchange, String method, String[] parts, String body) throws IOException {
            if ("POST".equals(method) && parts.length == 3 && "login".equals(parts[2])) {
                var response = authService.login(new app.auth.dto.LoginRequest(
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
                var response = authService.register(new app.auth.dto.RegisterRequest(
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

        private void handleServers(com.sun.net.httpserver.HttpExchange exchange, String method, String[] parts, String body) throws IOException {
            if ("GET".equals(method) && parts.length == 2) {
                send(exchange, 200, Responses.servers(serverCatalogService.listServers()));
                return;
            }

            if ("POST".equals(method) && parts.length == 2) {
                var request = new ServerRequest(
                    Json.readString(body, "name"),
                    Json.readString(body, "host"),
                    Json.readInt(body, "port", 25565),
                    Json.readString(body, "rconPassword"),
                    Json.readInt(body, "rconPort", 25575),
                    Json.readString(body, "serverProperties"),
                    Json.readString(body, "backupPath"),
                    minecraftDirectory
                );

                var createdServer = serverCatalogService.createServer(request, 1L);
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
                var server = serverCatalogService.getServer(serverId);
                if (server.isPresent()) {
                    send(exchange, 200, Responses.server(server.get()));
                } else {
                    send(exchange, 404, Responses.message("Server not found."));
                }
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

        private String readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            try (var inputStream = exchange.getRequestBody()) {
                return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        private void addCorsHeaders(com.sun.net.httpserver.HttpExchange exchange) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        }

        private void send(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] responseBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (var outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBytes);
            }
        }
    }
}
