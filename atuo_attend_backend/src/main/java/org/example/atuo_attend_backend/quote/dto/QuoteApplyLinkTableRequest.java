package org.example.atuo_attend_backend.quote.dto;

import java.util.List;

public class QuoteApplyLinkTableRequest {
    private List<Long> recordIds;

    public List<Long> getRecordIds() {
        return recordIds;
    }

    public void setRecordIds(List<Long> recordIds) {
        this.recordIds = recordIds;
    }
}
