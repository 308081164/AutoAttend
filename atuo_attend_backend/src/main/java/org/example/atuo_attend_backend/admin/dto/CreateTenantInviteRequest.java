package org.example.atuo_attend_backend.admin.dto;

public class CreateTenantInviteRequest {

    /** 默认可不传：最大使用次数，默认 1 */
    private Integer maxUses;
    /** 有效天数，默认 7 */
    private Integer expiresInDays;
    private String note;

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getExpiresInDays() {
        return expiresInDays;
    }

    public void setExpiresInDays(Integer expiresInDays) {
        this.expiresInDays = expiresInDays;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
