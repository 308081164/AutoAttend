package org.example.atuo_attend_backend.nexus.adapter.aliyun;

import com.aliyun.alidns20150109.Client;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsRequest;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponse;
import com.aliyun.alidns20150109.models.DescribeDomainsRequest;
import com.aliyun.alidns20150109.models.DescribeDomainsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 阿里云解析 DNS：DescribeDomains、DescribeDomainRecords（只读）。
 */
public class AliyunDnsAdapter {

    private static Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint("alidns.aliyuncs.com");
        return new Client(config);
    }

    public List<DomainRow> listDomains(String accessKeyId, String accessKeySecret, int pageSize) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret);
        RuntimeOptions runtime = new RuntimeOptions();
        DescribeDomainsRequest req = new DescribeDomainsRequest()
                .setPageSize((long) pageSize)
                .setPageNumber(1L);
        DescribeDomainsResponse resp = client.describeDomainsWithOptions(req, runtime);
        Map<String, Object> body = resp.getBody() != null ? resp.getBody().toMap() : Collections.emptyMap();
        Object domainsObj = body.get("Domains");
        if (!(domainsObj instanceof Map<?, ?> dm)) {
            return Collections.emptyList();
        }
        Object data = dm.get("Domain");
        if (!(data instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<DomainRow> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) continue;
            DomainRow r = new DomainRow();
            r.domainName = asString(m.get("DomainName"));
            if (r.domainName != null && !r.domainName.isBlank()) {
                out.add(r);
            }
        }
        return out;
    }

    public List<RecordRow> listRecords(String accessKeyId, String accessKeySecret, String domainName, int pageSize) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret);
        RuntimeOptions runtime = new RuntimeOptions();
        DescribeDomainRecordsRequest req = new DescribeDomainRecordsRequest()
                .setDomainName(domainName)
                .setPageSize((long) pageSize)
                .setPageNumber(1L);
        DescribeDomainRecordsResponse resp = client.describeDomainRecordsWithOptions(req, runtime);
        Map<String, Object> body = resp.getBody() != null ? resp.getBody().toMap() : Collections.emptyMap();
        Object recObj = body.get("DomainRecords");
        if (!(recObj instanceof Map<?, ?> rm)) {
            return Collections.emptyList();
        }
        Object data = rm.get("Record");
        if (!(data instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<RecordRow> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) continue;
            RecordRow r = new RecordRow();
            r.recordId = asString(m.get("RecordId"));
            r.rr = asString(m.get("RR"));
            r.type = asString(m.get("Type"));
            r.value = asString(m.get("Value"));
            if (m.get("TTL") != null) {
                try {
                    r.ttl = Integer.parseInt(String.valueOf(m.get("TTL")));
                } catch (NumberFormatException ignored) {
                    r.ttl = null;
                }
            }
            r.line = asString(m.get("Line"));
            if (r.recordId != null && !r.recordId.isBlank()) {
                out.add(r);
            }
        }
        return out;
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    public static class DomainRow {
        public String domainName;
    }

    public static class RecordRow {
        public String recordId;
        public String rr;
        public String type;
        public String value;
        public Integer ttl;
        public String line;
    }
}
