package org.example.atuo_attend_backend.platform.dto;

public class PlatformComponentClickRequest {
    private String componentKey;
    private String coreApiKey;

    public String getComponentKey() {
        return componentKey;
    }

    public void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
    }

    public String getCoreApiKey() {
        return coreApiKey;
    }

    public void setCoreApiKey(String coreApiKey) {
        this.coreApiKey = coreApiKey;
    }
}

