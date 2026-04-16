package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.admin.dto.TenantAdminUserItem;
import org.example.atuo_attend_backend.admin.dto.UpdateTenantAdminUserRequest;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.collab.service.CollabPasswordService;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminTenantAdminUserService {

    private final TenantAdminUserMapper tenantAdminUserMapper;
    private final BizUserMapper bizUserMapper;
    private final CollabPasswordService passwordService;

    public AdminTenantAdminUserService(TenantAdminUserMapper tenantAdminUserMapper,
                                       BizUserMapper bizUserMapper,
                                       CollabPasswordService passwordService) {
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.bizUserMapper = bizUserMapper;
        this.passwordService = passwordService;
    }

    public List<TenantAdminUserItem> listForTenant(long tenantId) {
        List<TenantAdminUser> rows = tenantAdminUserMapper.listByTenantId(tenantId);
        List<TenantAdminUserItem> out = new ArrayList<>(rows.size());
        for (TenantAdminUser u : rows) {
            TenantAdminUserItem it = new TenantAdminUserItem();
            it.setId(u.getId());
            it.setPhone(u.getPhone());
            it.setCanPublishProjectInfo(u.getCanPublishProjectInfo());
            it.setCreatedAt(u.getCreatedAt());
            out.add(it);
        }
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(long tenantId, long id, UpdateTenantAdminUserRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        boolean hasPhone = req.getPhone() != null && !req.getPhone().isBlank();
        boolean hasPwd = req.getNewPassword() != null && !req.getNewPassword().isBlank();
        boolean hasPublish = req.getCanPublishProjectInfo() != null;
        if (!hasPhone && !hasPwd && !hasPublish) {
            throw new IllegalArgumentException("请填写新手机号、新密码或发布权限");
        }
        TenantAdminUser tau = tenantAdminUserMapper.findById(id);
        if (tau == null || !tau.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("记录不存在或无权操作");
        }
        String oldPhone = tau.getPhone();
        boolean phoneChanged = false;

        if (hasPhone) {
            String newPhone = PhoneNormalizer.normalize(req.getPhone());
            if (newPhone == null) {
                throw new IllegalArgumentException("手机号格式不正确");
            }
            if (!newPhone.equals(oldPhone)) {
                if (tenantAdminUserMapper.countByPhoneExcludingId(newPhone, id) > 0) {
                    throw new IllegalArgumentException("该手机号已被其他管理员占用");
                }
                BizUser conflict = bizUserMapper.findByTenantAndEmail(tenantId, newPhone);
                if (conflict != null) {
                    BizUser oldBiz = bizUserMapper.findByTenantAndEmail(tenantId, oldPhone);
                    if (oldBiz == null || !oldBiz.getId().equals(conflict.getId())) {
                        throw new IllegalArgumentException("该手机号在协作侧已被占用");
                    }
                }
                int n = tenantAdminUserMapper.updatePhone(id, tenantId, newPhone);
                if (n != 1) {
                    throw new IllegalArgumentException("更新失败");
                }
                BizUser biz = bizUserMapper.findByTenantAndEmail(tenantId, oldPhone);
                if (biz != null) {
                    biz.setEmail(newPhone);
                    bizUserMapper.update(biz);
                }
                tau.setPhone(newPhone);
                phoneChanged = true;
            }
        }

        if (hasPwd) {
            String err = validatePassword(req.getNewPassword());
            if (err != null) {
                throw new IllegalArgumentException(err);
            }
            String hash = passwordService.hash(req.getNewPassword());
            int n = tenantAdminUserMapper.updatePasswordHash(id, tenantId, hash);
            if (n != 1) {
                throw new IllegalArgumentException("更新密码失败");
            }
            String phoneForBiz = tau.getPhone();
            BizUser biz = bizUserMapper.findByTenantAndEmail(tenantId, phoneForBiz);
            if (biz != null) {
                biz.setPasswordHash(hash);
                bizUserMapper.update(biz);
            }
        }

        if (hasPublish) {
            tenantAdminUserMapper.updateCanPublishProjectInfo(id, tenantId, Boolean.TRUE.equals(req.getCanPublishProjectInfo()));
        }

        if (!phoneChanged && !hasPwd && !hasPublish) {
            throw new IllegalArgumentException("无变更项");
        }
    }

    private static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.length() > 24) {
            return "密码长度不能超过 24 个字符";
        }
        return null;
    }
}
