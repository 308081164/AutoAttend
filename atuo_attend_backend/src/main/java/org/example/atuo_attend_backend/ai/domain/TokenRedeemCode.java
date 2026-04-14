package org.example.atuo_attend_backend.ai.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TokenRedeemCode {

    private Long id;
    private String codeHash;
    private BigDecimal grantCny;
    private int maxUses;
    private int usedCount;
    private LocalDateTime expiresAt;
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public BigDecimal getGrantCny() {
        return grantCny;
    }

    public void setGrantCny(BigDecimal grantCny) {
        this.grantCny = grantCny;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
