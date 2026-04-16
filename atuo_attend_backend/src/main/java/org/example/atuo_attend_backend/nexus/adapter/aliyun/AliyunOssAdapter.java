package org.example.atuo_attend_backend.nexus.adapter.aliyun;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.Bucket;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云 OSS：列举 Bucket（只读）。
 */
public class AliyunOssAdapter {

    public List<BucketRow> listBuckets(String accessKeyId, String accessKeySecret, String regionId) {
        String rid = regionId != null ? regionId.trim() : "cn-hangzhou";
        String endpoint = "https://oss-" + rid + ".aliyuncs.com";
        OSS oss = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            List<Bucket> buckets = oss.listBuckets();
            List<BucketRow> out = new ArrayList<>();
            if (buckets != null) {
                for (Bucket b : buckets) {
                    if (b == null) continue;
                    BucketRow row = new BucketRow();
                    row.name = b.getName();
                    row.location = b.getLocation();
                    row.region = rid;
                    if (row.name != null && !row.name.isBlank()) {
                        out.add(row);
                    }
                }
            }
            return out;
        } finally {
            oss.shutdown();
        }
    }

    public static class BucketRow {
        public String name;
        public String location;
        public String region;
    }
}
