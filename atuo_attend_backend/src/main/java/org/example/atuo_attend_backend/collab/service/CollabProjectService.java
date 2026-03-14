package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMemberMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollabProjectService {

    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final String ROLE_SUB_ADMIN = "sub_admin";

    private final BizProjectMapper projectMapper;
    private final BizProjectMemberMapper memberMapper;
    private final BizUserMapper userMapper;

    public CollabProjectService(BizProjectMapper projectMapper,
                                BizProjectMemberMapper memberMapper,
                                BizUserMapper userMapper) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
    }

    /**
     * 当前用户有权限的项目列表：超级管理员全部，子管理员仅被分配了管理权限的项目，普通成员仅参与的项目。
     */
    public List<BizProject> listProjectsForUser(long userId) {
        BizUser user = userMapper.findById(userId);
        if (user == null) return List.of();

        if (ROLE_SUPER_ADMIN.equals(user.getRole())) {
            return projectMapper.listAll();
        }
        if (ROLE_SUB_ADMIN.equals(user.getRole())) {
            List<Long> projectIds = memberMapper.listProjectIdsByUserIdAndRole(userId, "admin");
            return projectMapper.listAll().stream()
                    .filter(p -> projectIds.contains(p.getId()))
                    .collect(Collectors.toList());
        }
        List<Long> myProjectIds = memberMapper.listProjectIdsByUserId(userId);
        return projectMapper.listAll().stream()
                .filter(p -> myProjectIds.contains(p.getId()))
                .collect(Collectors.toList());
    }

    public BizProject getById(long projectId) {
        return projectMapper.findById(projectId);
    }

    public boolean canAccessProject(long userId, long projectId) {
        BizUser user = userMapper.findById(userId);
        if (user == null) return false;
        if (ROLE_SUPER_ADMIN.equals(user.getRole())) return true;
        List<Long> ids = memberMapper.listProjectIdsByUserId(userId);
        return ids.contains(projectId);
    }
}
