package org.example.atuo_attend_backend.collab.dto;

/**
 * CSV 导入会话：按批处理时的批次下标（0 开始）。
 */
public class CollabCsvChunkRequest {

    private int chunkIndex;

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
}
