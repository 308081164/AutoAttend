package org.example.atuo_attend_backend.nexus.service;

import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunEcsAdapter;
import org.example.atuo_attend_backend.nexus.crypto.NexusCryptoService;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudAccount;
import org.example.atuo_attend_backend.nexus.mapper.NexusCloudAccountMapper;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class NexusEcsOpsService {

    private final NexusCloudAccountMapper accountMapper;
    private final NexusCryptoService cryptoService;
    private final AliyunEcsAdapter ecsAdapter = new AliyunEcsAdapter();

    public NexusEcsOpsService(NexusCloudAccountMapper accountMapper, NexusCryptoService cryptoService) {
        this.accountMapper = accountMapper;
        this.cryptoService = cryptoService;
    }

    public void runLifecycle(long tenantId, long accountId, String instanceId, String action, boolean forceStop) throws Exception {
        NexusCloudAccount account = accountMapper.findForSync(tenantId, accountId);
        if (account == null) {
            throw new IllegalArgumentException("account not found");
        }
        if (!Objects.equals(account.getProvider(), "aliyun")) {
            throw new IllegalStateException("only aliyun supported");
        }
        String ak = cryptoService.decrypt(account.getAccessKeyIdEnc());
        String sk = cryptoService.decrypt(account.getAccessKeySecretEnc());
        String region = account.getRegionId();
        String a = action == null ? "" : action.trim().toLowerCase();
        switch (a) {
            case "start" -> ecsAdapter.startInstance(ak, sk, region, instanceId);
            case "stop" -> ecsAdapter.stopInstance(ak, sk, region, instanceId, forceStop);
            case "reboot" -> ecsAdapter.rebootInstance(ak, sk, region, instanceId, forceStop);
            default -> throw new IllegalArgumentException("invalid action: " + action);
        }
    }
}
