package org.example.atuo_attend_backend.nexus.adapter.aliyun;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.QuerySmsSignListRequest;
import com.aliyun.dysmsapi20170525.models.QuerySmsSignListResponse;
import com.aliyun.dysmsapi20170525.models.QuerySmsTemplateListRequest;
import com.aliyun.dysmsapi20170525.models.QuerySmsTemplateListResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 短信服务控制台资源只读：签名列表、模板列表（与业务验证码发送链路独立）。
 */
public class AliyunSmsMetaAdapter {

    private static Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint("dysmsapi.aliyuncs.com");
        return new Client(config);
    }

    public List<SignRow> listAllSigns(String accessKeyId, String accessKeySecret) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret);
        RuntimeOptions runtime = new RuntimeOptions();
        List<SignRow> all = new ArrayList<>();
        int page = 1;
        int pageSize = 50;
        while (true) {
            QuerySmsSignListRequest req = new QuerySmsSignListRequest().setPageIndex(page).setPageSize(pageSize);
            QuerySmsSignListResponse resp = client.querySmsSignListWithOptions(req, runtime);
            Map<String, Object> body = resp.getBody() != null ? resp.getBody().toMap() : Collections.emptyMap();
            Object listObj = body.get("SmsSignList");
            int added = 0;
            if (listObj instanceof List<?> list) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    SignRow r = new SignRow();
                    r.signName = asString(m.get("SignName"));
                    r.auditStatus = asString(m.get("AuditStatus"));
                    r.signType = asString(m.get("SignType"));
                    if (r.signName != null && !r.signName.isBlank()) {
                        all.add(r);
                        added++;
                    }
                }
            }
            if (added < pageSize) {
                break;
            }
            page++;
            if (page > 20) {
                break;
            }
        }
        return all;
    }

    public List<TemplateRow> listAllTemplates(String accessKeyId, String accessKeySecret) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret);
        RuntimeOptions runtime = new RuntimeOptions();
        List<TemplateRow> all = new ArrayList<>();
        int page = 1;
        int pageSize = 50;
        while (true) {
            QuerySmsTemplateListRequest req = new QuerySmsTemplateListRequest().setPageIndex(page).setPageSize(pageSize);
            QuerySmsTemplateListResponse resp = client.querySmsTemplateListWithOptions(req, runtime);
            Map<String, Object> body = resp.getBody() != null ? resp.getBody().toMap() : Collections.emptyMap();
            Object listObj = body.get("SmsTemplateList");
            int added = 0;
            if (listObj instanceof List<?> list) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    TemplateRow r = new TemplateRow();
                    r.templateCode = asString(m.get("TemplateCode"));
                    r.templateName = asString(m.get("TemplateName"));
                    r.templateType = asString(m.get("TemplateType"));
                    r.auditStatus = asString(m.get("AuditStatus"));
                    if (r.templateCode != null && !r.templateCode.isBlank()) {
                        all.add(r);
                        added++;
                    }
                }
            }
            if (added < pageSize) {
                break;
            }
            page++;
            if (page > 20) {
                break;
            }
        }
        return all;
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    public static class SignRow {
        public String signName;
        public String auditStatus;
        public String signType;
    }

    public static class TemplateRow {
        public String templateCode;
        public String templateName;
        public String templateType;
        public String auditStatus;
    }
}
