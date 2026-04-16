package org.example.atuo_attend_backend.quote.dto;

import java.util.Map;

/**
 * 租户工作台偏好（如侧栏是否展示报价入口）。
 */
public class WorkspacePrefsUpdateRequest {
    private Map<String, Object> prefs;

    public Map<String, Object> getPrefs() {
        return prefs;
    }

    public void setPrefs(Map<String, Object> prefs) {
        this.prefs = prefs;
    }
}
