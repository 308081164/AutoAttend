package org.example.atuo_attend_backend.nexus.service;

import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunDnsAdapter;
import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunOssAdapter;
import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunSmsMetaAdapter;
import org.example.atuo_attend_backend.nexus.mapper.NexusDnsDomainMapper;
import org.example.atuo_attend_backend.nexus.mapper.NexusDnsRecordMapper;
import org.example.atuo_attend_backend.nexus.mapper.NexusOssBucketMapper;
import org.example.atuo_attend_backend.nexus.mapper.NexusSmsSignatureMapper;
import org.example.atuo_attend_backend.nexus.mapper.NexusSmsTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步 DNS / OSS / 短信元数据到本地缓存表（失败不阻断主 ECS 同步）。
 */
@Service
public class NexusExtensionSyncService {

    private static final Logger log = LoggerFactory.getLogger(NexusExtensionSyncService.class);

    private final NexusDnsDomainMapper dnsDomainMapper;
    private final NexusDnsRecordMapper dnsRecordMapper;
    private final NexusOssBucketMapper ossBucketMapper;
    private final NexusSmsSignatureMapper smsSignatureMapper;
    private final NexusSmsTemplateMapper smsTemplateMapper;

    private final AliyunDnsAdapter dnsAdapter = new AliyunDnsAdapter();
    private final AliyunOssAdapter ossAdapter = new AliyunOssAdapter();
    private final AliyunSmsMetaAdapter smsMetaAdapter = new AliyunSmsMetaAdapter();

    public NexusExtensionSyncService(
            NexusDnsDomainMapper dnsDomainMapper,
            NexusDnsRecordMapper dnsRecordMapper,
            NexusOssBucketMapper ossBucketMapper,
            NexusSmsSignatureMapper smsSignatureMapper,
            NexusSmsTemplateMapper smsTemplateMapper
    ) {
        this.dnsDomainMapper = dnsDomainMapper;
        this.dnsRecordMapper = dnsRecordMapper;
        this.ossBucketMapper = ossBucketMapper;
        this.smsSignatureMapper = smsSignatureMapper;
        this.smsTemplateMapper = smsTemplateMapper;
    }

    public void syncExtensions(long tenantId, long accountId, String accessKeyId, String accessKeySecret, String regionId) {
        LocalDateTime syncedAt = LocalDateTime.now();
        syncDns(tenantId, accountId, accessKeyId, accessKeySecret, syncedAt);
        syncOss(tenantId, accountId, accessKeyId, accessKeySecret, regionId, syncedAt);
        syncSms(tenantId, accountId, accessKeyId, accessKeySecret, syncedAt);
    }

    private void syncDns(long tenantId, long accountId, String ak, String sk, LocalDateTime syncedAt) {
        try {
            dnsDomainMapper.deleteByAccount(tenantId, accountId);
            dnsRecordMapper.deleteByAccount(tenantId, accountId);
            List<AliyunDnsAdapter.DomainRow> domains = dnsAdapter.listDomains(ak, sk, 100);
            for (AliyunDnsAdapter.DomainRow d : domains) {
                if (d == null || d.domainName == null) continue;
                dnsDomainMapper.insert(tenantId, accountId, d.domainName.trim(), null, syncedAt);
                List<AliyunDnsAdapter.RecordRow> recs = dnsAdapter.listRecords(ak, sk, d.domainName.trim(), 100);
                for (AliyunDnsAdapter.RecordRow r : recs) {
                    dnsRecordMapper.insert(
                            tenantId, accountId, d.domainName.trim(),
                            r.recordId,
                            r.rr != null ? r.rr : "",
                            r.type != null ? r.type : "",
                            r.value != null ? r.value : "",
                            r.ttl,
                            r.line,
                            syncedAt
                    );
                }
            }
        } catch (Exception e) {
            log.warn("[nexus] dns extension sync failed tenant={} account={}: {}", tenantId, accountId, e.getMessage());
        }
    }

    private void syncOss(long tenantId, long accountId, String ak, String sk, String regionId, LocalDateTime syncedAt) {
        try {
            ossBucketMapper.deleteByAccount(tenantId, accountId);
            List<AliyunOssAdapter.BucketRow> buckets = ossAdapter.listBuckets(ak, sk, regionId);
            for (AliyunOssAdapter.BucketRow b : buckets) {
                ossBucketMapper.insert(tenantId, accountId, b.name, b.region, b.location, syncedAt);
            }
        } catch (Exception e) {
            log.warn("[nexus] oss extension sync failed tenant={} account={}: {}", tenantId, accountId, e.getMessage());
        }
    }

    private void syncSms(long tenantId, long accountId, String ak, String sk, LocalDateTime syncedAt) {
        try {
            smsSignatureMapper.deleteByAccount(tenantId, accountId);
            smsTemplateMapper.deleteByAccount(tenantId, accountId);
            for (AliyunSmsMetaAdapter.SignRow s : smsMetaAdapter.listAllSigns(ak, sk)) {
                smsSignatureMapper.insert(tenantId, accountId, s.signName, s.auditStatus, s.signType, syncedAt);
            }
            for (AliyunSmsMetaAdapter.TemplateRow t : smsMetaAdapter.listAllTemplates(ak, sk)) {
                smsTemplateMapper.insert(tenantId, accountId, t.templateCode, t.templateName, t.templateType, t.auditStatus, syncedAt);
            }
        } catch (Exception e) {
            log.warn("[nexus] sms meta sync failed tenant={} account={}: {}", tenantId, accountId, e.getMessage());
        }
    }
}
