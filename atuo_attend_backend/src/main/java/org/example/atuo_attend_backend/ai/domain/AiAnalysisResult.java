package org.example.atuo_attend_backend.ai.domain;

import java.time.LocalDateTime;

public class AiAnalysisResult {
    private Long id;
    private String repoFullName;
    private String commitSha;
    private String workSummary;
    private String workType;
    private String mainArea;
    private String isEffective;
    private String effectiveReason;
    private String invalidReasonTag;
    private String qualityLevel;
    private String qualityComment;
    private String riskFlags;
    private String suggestions;
    private String rawResponse;
    private String promptVersion;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public String getWorkSummary() { return workSummary; }
    public void setWorkSummary(String workSummary) { this.workSummary = workSummary; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public String getMainArea() { return mainArea; }
    public void setMainArea(String mainArea) { this.mainArea = mainArea; }
    public String getIsEffective() { return isEffective; }
    public void setIsEffective(String isEffective) { this.isEffective = isEffective; }
    public String getEffectiveReason() { return effectiveReason; }
    public void setEffectiveReason(String effectiveReason) { this.effectiveReason = effectiveReason; }
    public String getInvalidReasonTag() { return invalidReasonTag; }
    public void setInvalidReasonTag(String invalidReasonTag) { this.invalidReasonTag = invalidReasonTag; }
    public String getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }
    public String getQualityComment() { return qualityComment; }
    public void setQualityComment(String qualityComment) { this.qualityComment = qualityComment; }
    public String getRiskFlags() { return riskFlags; }
    public void setRiskFlags(String riskFlags) { this.riskFlags = riskFlags; }
    public String getSuggestions() { return suggestions; }
    public void setSuggestions(String suggestions) { this.suggestions = suggestions; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
