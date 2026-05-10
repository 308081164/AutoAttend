package org.example.atuo_attend_backend.collab.dto;

import java.util.List;

public class CollabCodingHeartbeatBatchRequest {

    private List<CollabCodingHeartbeatItemRequest> items;

    public List<CollabCodingHeartbeatItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CollabCodingHeartbeatItemRequest> items) {
        this.items = items;
    }
}
