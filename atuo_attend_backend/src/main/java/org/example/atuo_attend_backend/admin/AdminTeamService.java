package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.admin.dto.CreateMemberRequest;
import org.example.atuo_attend_backend.admin.dto.MemberProjectItem;
import org.example.atuo_attend_backend.admin.dto.UpdateMemberRequest;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectMember;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMemberMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.collab.service.CollabPasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminTeamService {

    private final BizUserMapper userMapper;
    private final BizProjectMapper projectMapper;
    private final BizProjectMemberMapper projectMemberMapper;
    private final CollabPasswordService passwordService;

    public AdminTeamService(BizUserMapper userMapper,
                            BizProjectMapper projectMapper,
                            BizProjectMemberMapper projectMemberMapper,
                            CollabPasswordService passwordService) {
        this.userMapper = userMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.passwordService = passwordService;
    }

    public List<BizUser> listMembers() {
        return userMapper.listAll();
    }

    public BizUser getMember(long id) {
        return userMapper.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public BizUser createMember(CreateMemberRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        String email = req.getEmail().trim();
        if (userMapper.findByEmail(email) != null) {
            throw new IllegalArgumentException("该邮箱已存在");
        }
        String password = req.getPassword() != null && !req.getPassword().isBlank()
                ? req.getPassword() : "123456";
        BizUser user = new BizUser();
        user.setEmail(email);
        user.setName(req.getName() != null ? req.getName().trim() : email);
        user.setPasswordHash(passwordService.hash(password));
        user.setRole("member");
        user.setRemarkName(req.getRemarkName() != null ? req.getRemarkName().trim() : null);
        user.setJobTitle(req.getJobTitle() != null ? req.getJobTitle().trim() : "开发工程师");
        userMapper.insert(user);
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMember(long id, UpdateMemberRequest req) {
        BizUser user = userMapper.findById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (req.getName() != null) user.setName(req.getName().trim());
        if (req.getRemarkName() != null) user.setRemarkName(req.getRemarkName().trim());
        if (req.getJobTitle() != null) user.setJobTitle(req.getJobTitle().trim());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar().trim().isEmpty() ? null : req.getAvatar().trim());
        if (req.getRole() != null && ("member".equals(req.getRole()) || "sub_admin".equals(req.getRole()) || "super_admin".equals(req.getRole()))) {
            user.setRole(req.getRole());
        }
        userMapper.update(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(long id, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        BizUser user = userMapper.findById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        user.setPasswordHash(passwordService.hash(newPassword));
        userMapper.update(user);
    }

    public List<BizProjectMember> getMemberProjects(long userId) {
        return projectMemberMapper.listByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setMemberProjects(long userId, List<MemberProjectItem> projects) {
        BizUser user = userMapper.findById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        List<Long> currentIds = projectMemberMapper.listProjectIdsByUserId(userId);
        for (Long projectId : currentIds) {
            projectMemberMapper.delete(projectId, userId);
        }
        if (projects != null) {
            for (MemberProjectItem item : projects) {
                if (item.getProjectId() == null) continue;
                BizProject p = projectMapper.findById(item.getProjectId());
                if (p == null) continue;
                String role = "admin".equals(item.getRole()) ? "admin" : "member";
                BizProjectMember m = new BizProjectMember();
                m.setProjectId(item.getProjectId());
                m.setUserId(userId);
                m.setRole(role);
                m.setSource("manual");
                projectMemberMapper.insert(m);
            }
        }
    }

    public List<BizProject> listAllProjects() {
        return projectMapper.listAll();
    }
}
