package app.server;

import domain.server.ManagedServer;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerFileService {
    private static final Pattern JAVA_ARGUMENT_PATTERN = Pattern.compile("(?i)(java(?:\\.exe)?\\s+)(.*?)(\\s+-jar\\s+.*)");

    private final ServerSupport support;

    public ServerFileService(ServerSupport support) {
        this.support = support;
    }

    public String readServerProperties(long serverId) {
        ManagedServer server = support.requireServer(serverId);
        Path file = support.requireMinecraftDirectory(server).resolve("server.properties");
        String content = support.readRequiredFile(file);
        server.setServerProperties(content);
        support.saveServer(server);
        return content;
    }

    public ManagedServer writeServerProperties(long serverId, String content) {
        ManagedServer server = support.requireServer(serverId);
        support.ensureServerStopped(server);
        Path file = support.requireMinecraftDirectory(server).resolve("server.properties");
        support.writeTextFile(file, content);
        server.setServerProperties(content);
        return support.saveServer(server);
    }

    public String readWhitelist(long serverId) {
        ManagedServer server = support.requireServer(serverId);
        return support.readRequiredFile(support.resolveWhitelistFile(support.requireMinecraftDirectory(server)));
    }

    public ManagedServer writeWhitelist(long serverId, String content) {
        ManagedServer server = support.requireServer(serverId);
        support.ensureServerStopped(server);
        support.writeTextFile(support.resolveWhitelistFile(support.requireMinecraftDirectory(server)), content);
        return support.saveServer(server);
    }

    public String readStartParameters(long serverId) {
        ManagedServer server = support.requireServer(serverId);
        Path startScript = support.resolveStartScript(support.requireMinecraftDirectory(server));
        Matcher matcher = JAVA_ARGUMENT_PATTERN.matcher(support.readRequiredFile(startScript));
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to find Java launch arguments in the start script.");
        }
        return matcher.group(2).trim();
    }

    public ManagedServer writeStartParameters(long serverId, String newParameters) {
        ManagedServer server = support.requireServer(serverId);
        support.ensureServerStopped(server);
        Path startScript = support.resolveStartScript(support.requireMinecraftDirectory(server));
        String script = support.readRequiredFile(startScript);
        Matcher matcher = JAVA_ARGUMENT_PATTERN.matcher(script);
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to find Java launch arguments in the start script.");
        }

        String replacement = matcher.group(1) + newParameters.trim() + matcher.group(3);
        String updatedScript = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        support.writeTextFile(startScript, updatedScript);
        return support.saveServer(server);
    }
}
