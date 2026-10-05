package com.yourteam.lostfound.dto;

import java.time.LocalDateTime;

public class NotificationResponseDTO {
    private Long id;
    private String message;
    private boolean read;
    private Long relatedItemId;
    private LocalDateTime createdAt;

    public NotificationResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Long getRelatedItemId() { return relatedItemId; }
    public void setRelatedItemId(Long relatedItemId) { this.relatedItemId = relatedItemId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}