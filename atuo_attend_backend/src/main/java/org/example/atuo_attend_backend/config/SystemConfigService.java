package org.example.atuo_attend_backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 系统配置（如 GitHub Token），供管理后台填写、GithubDiffFetcher 等使用。
 */
@Service
public class SystemConfigService {

    private static final String KEY_GITHUB_TOKEN = "github.token";
    private static final String KEY_GITHUB_API_PROXY = "github.api.proxy";
    /** 报价/合同：乙方（受托方）工商与收款信息 JSON，管理后台维护 */
    public static final String KEY_QUOTE_PARTY_B_PROFILE = "quote.party_b_profile_json";

    private final SystemConfigMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SystemConfigService(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    /** 获取 GitHub Token 原始值（供拉取 Diff 使用）；未配置返回 null。 */
    public String getGitHubToken() {
        String v = mapper.findByKey(KEY_GITHUB_TOKEN);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    /** 脱敏展示：前 4 位 + **** + 后 4 位；无则返回 null。 */
    public String getGitHubTokenMasked() {
        String v = getGitHubToken();
        if (v == null || v.length() <= 8) return v != null && !v.isBlank() ? "****" : null;
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }

    /** 传入新 token 则更新，传入空字符串则清空；含 **** 的脱敏串视为未修改不更新。 */
    public void setGitHubToken(String token) {
        if (token != null && token.contains("****")) return;
        String value = token == null ? "" : token.trim();
        mapper.upsert(KEY_GITHUB_TOKEN, value);
    }

    public String getGitHubApiProxy() {
        String v = mapper.findByKey(KEY_GITHUB_API_PROXY);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public void setGitHubApiProxy(String proxy) {
        String value = (proxy != null && !proxy.isBlank()) ? proxy.trim() : null;
        mapper.upsert(KEY_GITHUB_API_PROXY, value != null ? value : "");
    }

    /**
     * 乙方（开发方）主体模板 JSON：
     * 法人/组织：legalName、creditCode、address、contactName、contactPhone、bankName、bankAccount；
     * 自然人：嵌套对象 naturalPerson（fullName、idNumber、address、contactPhone、bankName、bankAccount、email 等）。
     */
    public Map<String, Object> getQuotePartyBProfile() {
        String raw = mapper.findByKey(KEY_QUOTE_PARTY_B_PROFILE);
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 与已有 JSON 合并写入：仅更新请求体中出现的顶层字段；{@code naturalPerson} 为 Map 时与库内同名对象做浅合并，
     * 避免「只保存法人模板」时清空自然人模板（及反向）。
     */
    @SuppressWarnings("unchecked")
    public void saveQuotePartyBProfile(Map<String, Object> incoming) throws JsonProcessingException {
        if (incoming == null) {
            return;
        }
        Map<String, Object> merged = new LinkedHashMap<>(getQuotePartyBProfile());
        for (Map.Entry<String, Object> e : incoming.entrySet()) {
            String k = e.getKey();
            if (Objects.equals("naturalPerson", k) && e.getValue() instanceof Map<?, ?> rawNp) {
                Map<String, Object> npBase = new LinkedHashMap<>();
                Object oldNp = merged.get("naturalPerson");
                if (oldNp instanceof Map<?, ?> om) {
                    for (Map.Entry<?, ?> ie : om.entrySet()) {
                        if (ie.getKey() != null) {
                            npBase.put(ie.getKey().toString(), ie.getValue());
                        }
                    }
                }
                for (Map.Entry<?, ?> ie : rawNp.entrySet()) {
                    if (ie.getKey() != null) {
                        npBase.put(ie.getKey().toString(), ie.getValue());
                    }
                }
                merged.put("naturalPerson", npBase);
            } else if (!Objects.equals("naturalPerson", k)) {
                merged.put(k, e.getValue());
            }
        }
        String json = merged.isEmpty() ? "{}" : objectMapper.writeValueAsString(merged);
        mapper.upsert(KEY_QUOTE_PARTY_B_PROFILE, json);
    }
}
