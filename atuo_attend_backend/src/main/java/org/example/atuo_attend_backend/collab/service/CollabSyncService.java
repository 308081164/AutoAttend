package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.domain.*;
import org.example.atuo_attend_backend.collab.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 协作模块同步：Webhook 推送时同步用户、项目（随仓库自动创建）、项目成员。
 */
@Service
public class CollabSyncService {

    private static final Logger log = LoggerFactory.getLogger(CollabSyncService.class);
    private static final String DEFAULT_PASSWORD = "123456";

    private final BizUserMapper userMapper;
    private final BizProjectMapper projectMapper;
    private final BizProjectMemberMapper memberMapper;
    private final BizProjectTableMapper tableMapper;
    private final BizTableColumnMapper columnMapper;
    private final BizOptionGroupMapper optionGroupMapper;
    private final CollabPasswordService passwordService;

    public CollabSyncService(BizUserMapper userMapper,
                             BizProjectMapper projectMapper,
                             BizProjectMemberMapper memberMapper,
                             BizProjectTableMapper tableMapper,
                             BizTableColumnMapper columnMapper,
                             BizOptionGroupMapper optionGroupMapper,
                             CollabPasswordService passwordService) {
        this.userMapper = userMapper;
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.tableMapper = tableMapper;
        this.columnMapper = columnMapper;
        this.optionGroupMapper = optionGroupMapper;
        this.passwordService = passwordService;
    }

    /**
     * 根据 commit 作者邮箱确保用户存在；不存在则创建（默认密码 123456）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizUser ensureUser(String email, String name) {
        if (email == null || email.isBlank()) return null;
        BizUser user = userMapper.findByEmail(email.trim());
        if (user != null) {
            if (name != null && !name.equals(user.getName())) {
                user.setName(name);
                userMapper.update(user);
            }
            return user;
        }
        user = new BizUser();
        user.setEmail(email.trim());
        user.setName(name != null ? name.trim() : email);
        user.setPasswordHash(passwordService.hash(DEFAULT_PASSWORD));
        user.setRole("member");
        user.setJobTitle("开发工程师");
        userMapper.insert(user);
        log.info("Collab user created: email={}", user.getEmail());
        return user;
    }

    /**
     * 根据仓库 full_name 确保项目及多维表存在；不存在则创建项目+表+默认列。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizProject ensureProjectAndTable(String repoFullName) {
        if (repoFullName == null || repoFullName.isBlank()) return null;
        String repoId = repoFullName.trim();
        BizProject project = projectMapper.findByRepoId(repoId);
        if (project != null) return project;

        project = new BizProject();
        project.setName(repoId);
        project.setDescription("");
        project.setRepoId(repoId);
        project.setStatus("active");
        projectMapper.insert(project);

        BizProjectTable table = new BizProjectTable();
        table.setProjectId(project.getId());
        table.setName("任务表");
        tableMapper.insert(table);

        createDefaultColumnsAndOptionGroups(project.getId(), table.getId());
        log.info("Collab project and table created: repoId={}, projectId={}", repoId, project.getId());
        return project;
    }

    private void createDefaultColumnsAndOptionGroups(long projectId, long tableId) {
        // 全局选项组：重要程度、解决情况、验收结果
        BizOptionGroup important = new BizOptionGroup();
        important.setName("重要程度");
        important.setOptions("[\"严重紧急\",\"阶段性优先解决\",\"下一阶段待办\",\"等待排期\"]");
        important.setScope("global");
        important.setProjectId(null);
        optionGroupMapper.insert(important);

        BizOptionGroup resolve = new BizOptionGroup();
        resolve.setName("当前状态");
        resolve.setOptions("[\"已创建\",\"开发中\",\"修复中\",\"待测试\",\"测试中\",\"已验收\"]");
        resolve.setScope("global");
        resolve.setProjectId(null);
        optionGroupMapper.insert(resolve);

        BizOptionGroup accept = new BizOptionGroup();
        accept.setName("验收结果");
        accept.setOptions("[\"未验收\",\"待验收\",\"通过任务关闭\"]");
        accept.setScope("global");
        accept.setProjectId(null);
        optionGroupMapper.insert(accept);

        // 归属模块：按项目配置，先给空选项
        BizOptionGroup module = new BizOptionGroup();
        module.setName("归属模块");
        module.setOptions("[]");
        module.setScope("project");
        module.setProjectId(projectId);
        optionGroupMapper.insert(module);

        int order = 0;
        insertColumn(tableId, "问题描述", "text", null, order++);
        insertColumn(tableId, "归属模块", "single_select", module.getId(), order++);
        insertColumn(tableId, "图像展示", "attachment", null, order++);
        insertColumn(tableId, "负责人", "multi_user", null, order++);
        insertColumn(tableId, "重要程度", "single_select", important.getId(), order++);
        insertColumn(tableId, "创建人", "text", null, order++);
        insertColumn(tableId, "当前状态", "single_select", resolve.getId(), order++);
        insertColumn(tableId, "验收结果", "single_select", accept.getId(), order++);
        insertColumn(tableId, "创建时间", "datetime", null, order++);
    }

    private void insertColumn(long tableId, String name, String columnType, Long optionGroupId, int sortOrder) {
        BizTableColumn col = new BizTableColumn();
        col.setTableId(tableId);
        col.setName(name);
        col.setColumnType(columnType);
        col.setOptionGroupId(optionGroupId);
        col.setSortOrder(sortOrder);
        columnMapper.insert(col);
    }

    /**
     * 确保用户为项目成员（仓库作者同步，source=sync）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void ensureProjectMember(long projectId, long userId) {
        if (memberMapper.findByProjectAndUser(projectId, userId) != null) return;
        BizProjectMember member = new BizProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole("member");
        member.setSource("sync");
        memberMapper.insert(member);
    }
}
