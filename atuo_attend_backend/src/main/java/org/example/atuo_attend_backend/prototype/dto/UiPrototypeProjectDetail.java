package org.example.atuo_attend_backend.prototype.dto;

import java.util.List;

public class UiPrototypeProjectDetail {
    private Long id;
    private String name;
    private Integer currentSpecVersion;
    private List<UiPrototypeSpecItem> specs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCurrentSpecVersion() { return currentSpecVersion; }
    public void setCurrentSpecVersion(Integer currentSpecVersion) { this.currentSpecVersion = currentSpecVersion; }

    public List<UiPrototypeSpecItem> getSpecs() { return specs; }
    public void setSpecs(List<UiPrototypeSpecItem> specs) { this.specs = specs; }
}

