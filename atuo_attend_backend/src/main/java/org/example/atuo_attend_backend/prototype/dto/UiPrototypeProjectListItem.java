package org.example.atuo_attend_backend.prototype.dto;

import java.time.LocalDateTime;

public class UiPrototypeProjectListItem {
    private Long id;
    private String name;
    private Integer currentSpecVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCurrentSpecVersion() { return currentSpecVersion; }
    public void setCurrentSpecVersion(Integer currentSpecVersion) { this.currentSpecVersion = currentSpecVersion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

