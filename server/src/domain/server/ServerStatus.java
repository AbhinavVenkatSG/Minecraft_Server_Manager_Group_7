/**
 * @file ServerStatus.java
 * @brief Enumeration of possible Minecraft server states.
 * @{
 */

package domain.server;

/**
 * @enum ServerStatus
 * @brief Possible runtime states for a managed Minecraft server.
 */
public enum ServerStatus {
    STARTUP,
    RUNNING,
    STOPPED,
    BLOCKED
}
