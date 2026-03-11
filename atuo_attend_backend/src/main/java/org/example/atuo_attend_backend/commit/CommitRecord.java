package org.example.atuo_attend_backend.commit;

import java.time.OffsetDateTime;

public class CommitRecord {

    private Long id;
    private String repoFullName;
    private String commitSha;
    private String parentSha;
    private String authorName;
    private String authorEmail;
    private OffsetDateTime committedAt;
    private String message;
    private int filesChanged;
    private int insertions;
    private int deletions;
    private boolean validCommit;
    private String validReason;
    private String diffText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepoFullName() {
        return repoFullName;
    }

    public void setRepoFullName(String repoFullName) {
        this.repoFullName = repoFullName;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getParentSha() {
        return parentSha;
    }

    public void setParentSha(String parentSha) {
        this.parentSha = parentSha;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public OffsetDateTime getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(OffsetDateTime committedAt) {
        this.committedAt = committedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getFilesChanged() {
        return filesChanged;
    }

    public void setFilesChanged(int filesChanged) {
        this.filesChanged = filesChanged;
    }

    public int getInsertions() {
        return insertions;
    }

    public void setInsertions(int insertions) {
        this.insertions = insertions;
    }

    public int getDeletions() {
        return deletions;
    }

    public void setDeletions(int deletions) {
        this.deletions = deletions;
    }

    public boolean isValidCommit() {
        return validCommit;
    }

    public void setValidCommit(boolean validCommit) {
        this.validCommit = validCommit;
    }

    public String getValidReason() {
        return validReason;
    }

    public void setValidReason(String validReason) {
        this.validReason = validReason;
    }

    public String getDiffText() {
        return diffText;
    }

    public void setDiffText(String diffText) {
        this.diffText = diffText;
    }
}

