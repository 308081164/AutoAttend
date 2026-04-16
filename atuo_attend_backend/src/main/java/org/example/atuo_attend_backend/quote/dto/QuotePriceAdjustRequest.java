package org.example.atuo_attend_backend.quote.dto;

import java.math.BigDecimal;

/**
 * 对最近一次报价结果做商务等比例调价（作用于未标记「不参与缩放」的明细行）。
 */
public class QuotePriceAdjustRequest {
    /** 目标商务总价（与 scaleFactor 二选一，优先 targetAmount） */
    private BigDecimal targetAmount;
    /** 直接指定系数，如 0.92 表示整体 92% */
    private BigDecimal scaleFactor;
    /** 可选说明 */
    private String note;

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(BigDecimal scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
