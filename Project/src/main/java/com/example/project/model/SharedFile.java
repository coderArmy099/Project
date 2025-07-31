package com.example.project.model;

import java.time.LocalDateTime;

public class SharedFile {
    private String fileId;
    private String originalName;
    private String uploaderUsername;
    private long fileSize;
    private LocalDateTime uploadTime;

    public SharedFile(String fileId, String originalName, String uploaderUsername, long fileSize) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.uploaderUsername = uploaderUsername;
        this.fileSize = fileSize;
        this.uploadTime = LocalDateTime.now();
    }

    // Getters
    public String getFileId() { return fileId; }
    public String getOriginalName() { return originalName; }
    public String getUploaderUsername() { return uploaderUsername; }
    public long getFileSize() { return fileSize; }
    public LocalDateTime getUploadTime() { return uploadTime; }
}
