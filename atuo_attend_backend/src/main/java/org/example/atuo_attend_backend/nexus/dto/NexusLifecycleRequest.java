package org.example.atuo_attend_backend.nexus.dto;

public class NexusLifecycleRequest {
    /** start | stop | reboot */
    private String action;
    private Boolean forceStop;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Boolean getForceStop() {
        return forceStop;
    }

    public void setForceStop(Boolean forceStop) {
        this.forceStop = forceStop;
    }
}
