package app.server;

import domain.server.ManagedServer;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes server-owned files such as {@code server.properties} and whitelist data.
 */
public class ServerFileService {
    private static final Pattern JAVA_ARGUMENT_PATTERN = Pattern.compile("(?i)(java(?:\\.exe)?\\s+)(.*?)(\\s+-jar\\s+.*)");

    private final ServerSupport support;

    /**
     * Creates a file service backed by shared server helpers.
     *
     * @param support shared server helpers and runtime state
     */
    public ServerFileService(ServerSupport support) {
        this.support = support;
    }

    /**
     * Reads {@code server.properties} from disk and mirrors it back into persisted server state.
     *
     * @param serverId managed server id
     * @return file contents
     */
    public String readServerProperties(long serverId) {
        ManagedServer server = support.requireServer(serverId);
        Path file = support.requireMinecraftDirectory(server).resolve("server.properties");
        String content = support.readRequiredFile(file);
        server.setServerProperties(content);
        support.saveServer(server);
        return content;
    }

    /**
     * Writes {@code server.properties} while the server is stopped.
     *
     * @param serverId managed server id
     * @param content new file contents
     * @return the updated server record
     */
    public ManagedServer writeServerProperties(long serverId, String content) {
        ManagedServer server = support.requireServer(serverId);
        support.ensureServerStopped(server);
        Path file = support.requireMinecraftDirectory(server).resolve("server.properties");
        support.writeTextFile(file, content);
        server.setServerProperties(content);
        return support.saveServer(server);
    }

    /**
     * Reads the whitelist file from the Minecraft directory.
     *
     * @param serverId managed server id
     * @return whitelist file contents
     */
    public String readWhitelist(long serverId) {
        ManagedServer server = support.requireServer(serverId);
        return support.readRequiredFile(support.resolveWhitelistFile(support.requireMinecraftDirectory(server)));
    }

    /**
     * Writes the whitelist file while the server is stopped.
     *
     * @param serverId managed server id
     * @param content new whitelist contents
     * @return the updated server record
     */
    public ManagedServer writeWhitelist(long serverId, String content) {
        ManagedServer server = support.requireServer(serverId);
        support.ensureServerStopped(server);
        support.writeTextFile(support.resolveWhitelistFile(support.requireMinecraftDirectory(server)), content);
        return support.saveServer(server);
    }

    /**
     * Extracts the JVM argument segment from the launch script.
     *
     * @param serverId managed server id
     * @return the arguments passed to {@code java} before {@code -jar}
     */
    public String readStartParameters(long serverId) {
        ManagedServer server = support.requireServer(serverId);
        Path startScript = support.resolveStartScript(support.requireMinecraftDirectory(server));
        Matcher matcher = JAVA_ARGUMENT_PATTERN.matcher(support.readRequiredFile(startScript));
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to find Java launch arguments in the start script.");
        }
        return matcher.group(2).trim();
    }

    /**
     * Replaces only the JVM argument portion of the launch script.
     *
     * @param serverId managed server id
     * @param newParameters new JVM argument string
     * @return the updated server record
     */
    public ManagedServer writeStartParameters(long serverId, String newParameters) {
        ManagedServer server = support.requireServer(serverId);
        support.ensureServerStopped(server);
        Path startScript = support.resolveStartScript(support.requireMinecraftDirectory(server));
        String script = support.readRequiredFile(startScript);
        Matcher matcher = JAVA_ARGUMENT_PATTERN.matcher(script);
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to find Java launch arguments in the start script.");
        }

        // Keep the launcher command and jar target intact while swapping only the tunable JVM flags.
        String replacement = matcher.group(1) + newParameters.trim() + matcher.group(3);
        String updatedScript = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        support.writeTextFile(startScript, updatedScript);
        return support.saveServer(server);
    }
}
