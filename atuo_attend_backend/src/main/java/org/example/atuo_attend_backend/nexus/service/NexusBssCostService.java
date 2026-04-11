package org.example.atuo_attend_backend.nexus.service;

import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunBssAdapter;
import org.example.atuo_attend_backend.nexus.crypto.NexusCryptoService;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudAccount;
import org.example.atuo_attend_backend.nexus.mapper.NexusCloudAccountMapper;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class NexusBssCostService {

    private final NexusCloudAccountMapper accountMapper;
    private final NexusCryptoService cryptoService;
    private final AliyunBssAdapter bssAdapter = new AliyunBssAdapter();

    public NexusBssCostService(NexusCloudAccountMapper accountMapper, NexusCryptoService cryptoService) {
        this.accountMapper = accountMapper;
        this.cryptoService = cryptoService;
    }

    public AliyunBssAdapter.CostSummary summarizeForAccount(long tenantId, long accountId, String billingCycle, int topN) throws Exception {
        NexusCloudAccount account = accountMapper.findForSync(tenantId, accountId);
        if (account == null) {
            throw new IllegalArgumentException("account not found");
        }
        if (!Objects.equals(account.getProvider(), "aliyun")) {
            throw new IllegalStateException("only aliyun supported");
        }
        String ak = cryptoService.decrypt(account.getAccessKeyIdEnc());
        String sk = cryptoService.decrypt(account.getAccessKeySecretEnc());
        if (billingCycle == null || !billingCycle.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("billingCycle must be YYYY-MM");
        }
        return bssAdapter.summarizeEcsByInstance(ak, sk, billingCycle.trim(), topN);
    }
}
