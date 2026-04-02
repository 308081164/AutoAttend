package org.example.atuo_attend_backend.prototype.dto;

public class UiPrototypeGenerateMockupRequest {
    private String prompt;
    private String model;
    private String messagesJson;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getMessagesJson() { return messagesJson; }
    public void setMessagesJson(String messagesJson) { this.messagesJson = messagesJson; }
}

