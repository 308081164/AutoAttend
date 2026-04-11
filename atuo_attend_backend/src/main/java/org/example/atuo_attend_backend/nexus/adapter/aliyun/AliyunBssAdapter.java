package org.example.atuo_attend_backend.nexus.adapter.aliyun;

import com.aliyun.bssopenapi20171214.Client;
import com.aliyun.bssopenapi20171214.models.QueryAccountBillRequest;
import com.aliyun.bssopenapi20171214.models.QueryAccountBillResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云费用中心 OpenAPI（BssOpenApi 2017-12-14）：按账期拉取账单明细并聚合 ECS 实例维度费用。
 * 说明：账单字段以阿里云返回为准，不同产品行项目可能无 InstanceID，仅作成本参考。
 */
public class AliyunBssAdapter {

    public static class InstanceCostRow {
        public String instanceId;
        public String instanceName;
        public double amount;

        public InstanceCostRow(String instanceId, String instanceName, double amount) {
            this.instanceId = instanceId;
            this.instanceName = instanceName;
            this.amount = amount;
        }
    }

    public static class CostSummary {
        public double totalPretax;
        public List<InstanceCostRow> topByInstance = new ArrayList<>();
    }

    public CostSummary summarizeEcsByInstance(String accessKeyId, String accessKeySecret, String billingCycle, int topN) throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.endpoint = "business.aliyuncs.com";
        Client client = new Client(config);
        RuntimeOptions runtime = new RuntimeOptions();

        Map<String, Agg> aggByInstance = new HashMap<>();
        double totalAll = 0d;

        int pageNum = 1;
        final int pageSize = 300;
        final int maxPages = 40;

        while (pageNum <= maxPages) {
            QueryAccountBillRequest req = new QueryAccountBillRequest()
                    .setBillingCycle(billingCycle)
                    .setPageNum(pageNum)
                    .setPageSize(pageSize);
            QueryAccountBillResponse resp = client.queryAccountBillWithOptions(req, runtime);
            Map<String, Object> root = resp.body.toMap();
            Object dataObj = root.get("Data");
            if (!(dataObj instanceof Map<?, ?> data)) break;

            Object itemsObj = data.get("Items");
            List<Map<String, Object>> flatItems = flattenItems(itemsObj);
            if (flatItems.isEmpty()) break;

            for (Map<String, Object> item : flatItems) {
                String product = stringVal(item.get("ProductCode"));
                String productName = stringVal(item.get("ProductName"));
                String productDetail = stringVal(item.get("ProductDetail"));
                boolean ecsLike = isEcsLike(product, productName, productDetail, item);
                if (!ecsLike) {
                    continue;
                }
                double amt = parseAmount(item);
                totalAll += amt;
                String iid = firstNonBlank(
                        firstNonBlank(stringVal(item.get("InstanceID")), stringVal(item.get("InstanceId"))),
                        stringVal(item.get("ResourceId"))
                );
                final String aggKey;
                if (iid == null || iid.isBlank() || !iid.startsWith("i-")) {
                    aggKey = "_unallocated";
                } else {
                    aggKey = iid;
                }
                String iname = firstNonBlank(stringVal(item.get("NickName")), stringVal(item.get("InstanceName")));
                Agg g = aggByInstance.computeIfAbsent(aggKey, k -> new Agg(aggKey, iname));
                g.amount += amt;
                if (iname != null && !iname.isBlank()) {
                    g.name = iname;
                }
            }

            Object totalObj = data.get("TotalCount");
            int totalCount = totalObj instanceof Number n ? n.intValue() : -1;
            if (flatItems.size() < pageSize) {
                break;
            }
            if (totalCount > 0 && pageNum * pageSize >= totalCount) {
                break;
            }
            pageNum++;
        }

        CostSummary out = new CostSummary();
        out.totalPretax = totalAll;
        List<Agg> sorted = new ArrayList<>(aggByInstance.values());
        sorted.sort(Comparator.comparingDouble((Agg a) -> a.amount).reversed());
        int n = Math.max(1, Math.min(topN, 100));
        for (int i = 0; i < sorted.size() && i < n; i++) {
            Agg a = sorted.get(i);
            out.topByInstance.add(new InstanceCostRow(a.id, a.name, round2(a.amount)));
        }
        return out;
    }

    private static boolean isEcsLike(String productCode, String productName, String productDetail, Map<String, Object> item) {
        String pc = productCode != null ? productCode.toLowerCase() : "";
        if (pc.contains("ecs")) return true;
        String pn = (productName != null ? productName : "").toLowerCase();
        String pd = (productDetail != null ? productDetail : "").toLowerCase();
        if (pn.contains("云服务器") || pn.contains("elastic compute")) return true;
        if (pd.contains("云服务器") || pd.contains("ecs")) return true;
        String service = stringVal(item.get("ServiceType"));
        if (service != null && service.toLowerCase().contains("ecs")) return true;
        return false;
    }

    private static List<Map<String, Object>> flattenItems(Object itemsObj) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (itemsObj instanceof Map<?, ?> m) {
            Object item = m.get("Item");
            if (item instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> im) {
                        out.add(castMap(im));
                    }
                }
            } else if (item instanceof Map<?, ?> single) {
                out.add(castMap(single));
            }
        } else if (itemsObj instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> im) {
                    out.add(castMap(im));
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> m) {
        Map<String, Object> r = new HashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            r.put(String.valueOf(e.getKey()), e.getValue());
        }
        return r;
    }

    private static double parseAmount(Map<String, Object> item) {
        Object v = item.get("PretaxAmount");
        if (v == null) v = item.get("PaymentAmount");
        if (v == null) v = item.get("AfterDiscountAmount");
        if (v == null) v = item.get("OutstandingAmount");
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0d;
        }
    }

    private static String stringVal(Object a) {
        if (a == null) return null;
        String s = String.valueOf(a);
        return s.isBlank() ? null : s;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private static double round2(double v) {
        return Math.round(v * 100d) / 100d;
    }

    private static class Agg {
        String id;
        String name;
        double amount;

        Agg(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
