package org.example.atuo_attend_backend.prototype.penpot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 输出的 Penpot 布局计划（简化：标题条 + 若干内容块）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenpotLayoutPlan {

    private String title = "快原型";
    private String subtitle = "";
    @JsonProperty("boardWidth")
    private double boardWidth = 1440;
    @JsonProperty("boardHeight")
    private double boardHeight = 900;
    private List<Block> blocks = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public double getBoardWidth() { return boardWidth; }
    public void setBoardWidth(double boardWidth) { this.boardWidth = boardWidth; }

    public double getBoardHeight() { return boardHeight; }
    public void setBoardHeight(double boardHeight) { this.boardHeight = boardHeight; }

    public List<Block> getBlocks() { return blocks; }
    public void setBlocks(List<Block> blocks) { this.blocks = blocks; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Block {
        private String label = "";
        private String body = "";

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
}
