package domain.backup;

import java.time.LocalDateTime;

public class Backup {
    private long id;
    private final long serverId;
    private final String filename;
    private final String filePath;
    private final long fileSize;
    private final LocalDateTime createdAt;

    public Backup(long id, long serverId, String filename, String filePath, long fileSize, LocalDateTime createdAt) {
        this.id = id;
        this.serverId = serverId;
        this.filename = filename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getServerId() {
        return serverId;
    }

    public String getFilename() {
        return filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
