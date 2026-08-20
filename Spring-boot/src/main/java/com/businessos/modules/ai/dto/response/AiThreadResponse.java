package com.businessos.modules.ai.dto.response;

import com.businessos.modules.ai.enums.AiFeature;
import java.time.LocalDateTime;

public class AiThreadResponse {
    private Long id;
    private AiFeature feature;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AiFeature getFeature() { return feature; }
    public void setFeature(AiFeature feature) { this.feature = feature; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
