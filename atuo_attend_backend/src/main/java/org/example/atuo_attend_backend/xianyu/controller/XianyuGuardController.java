package org.example.atuo_attend_backend.xianyu.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.xianyu.domain.*;
import org.example.atuo_attend_backend.xianyu.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 咸鱼值守 Controller
 * 提供账号管理、会话查看、消息查看、快捷回复等 API
 */
@RestController
@RequestMapping("/api/admin/xianyu")
public class XianyuGuardController {

    @Autowired
    private XianyuAccountMapper accountMapper;

    @Autowired
    private XianyuConversationMapper conversationMapper;

    @Autowired
    private XianyuMessageMapper messageMapper;

    @Autowired
    private XianyuQuickReplyMapper quickReplyMapper;

    // ==================== 账号管理 ====================

    @GetMapping("/accounts")
    public ApiResponse<List<XianyuAccount>> listAccounts() {
        long tenantId = TenantContext.requireTenantId();
        return ApiResponse.ok(accountMapper.selectByTenantId(tenantId));
    }

    @PostMapping("/accounts")
    public ApiResponse<XianyuAccount> createAccount(@RequestBody XianyuAccount account) {
        long tenantId = TenantContext.requireTenantId();
        account.setTenantId(tenantId);
        account.setStatus("offline");
        account.setLastLoginAt(LocalDateTime.now());
        accountMapper.insert(account);
        return ApiResponse.ok(account);
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<XianyuAccount> updateAccount(@PathVariable Long id, @RequestBody XianyuAccount account) {
        account.setId(id);
        accountMapper.update(account);
        return ApiResponse.ok(accountMapper.selectById(id));
    }

    @DeleteMapping("/accounts/{id}")
    public ApiResponse<Void> deleteAccount(@PathVariable Long id) {
        accountMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/accounts/{id}/status")
    public ApiResponse<Void> updateAccountStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        accountMapper.updateStatus(id, status);
        return ApiResponse.ok(null);
    }

    @PostMapping("/accounts/{id}/heartbeat")
    public ApiResponse<Void> heartbeat(@PathVariable Long id) {
        accountMapper.updateActiveTime(id);
        return ApiResponse.ok(null);
    }

    /** 插入一条测试会话与买家消息，便于无真实接入时验证 UI */
    @PostMapping("/accounts/{accountId}/demo-thread")
    public ApiResponse<Map<String, Object>> seedDemoThread(@PathVariable Long accountId) {
        long tenantId = TenantContext.requireTenantId();
        XianyuAccount acc = accountMapper.selectById(accountId);
        if (acc == null || !Objects.equals(acc.getTenantId(), tenantId)) {
            return ApiResponse.error(404, "账号不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        XianyuConversation conv = new XianyuConversation();
        conv.setAccountId(accountId);
        conv.setPeerId("demo_peer_" + System.currentTimeMillis());
        conv.setPeerNickname("模拟买家（测试）");
        conv.setPeerAvatar(null);
        conv.setLastMessage("您好，这个还在吗？");
        conv.setLastMessageAt(now);
        conv.setUnreadCount(1);
        conv.setStatus("active");
        conversationMapper.insert(conv);

        XianyuMessage m = new XianyuMessage();
        m.setConversationId(conv.getId());
        m.setDirection("in");
        m.setContent("您好，这个还在吗？想了解一下发货时间。");
        m.setMsgType("text");
        m.setSentAt(now.minusMinutes(2));
        messageMapper.insert(m);

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conv.getId());
        return ApiResponse.ok(data);
    }

    // ==================== 会话管理 ====================

    @GetMapping("/accounts/{accountId}/conversations")
    public ApiResponse<List<XianyuConversation>> listConversations(@PathVariable Long accountId) {
        return ApiResponse.ok(conversationMapper.selectActiveByAccountId(accountId));
    }

    @GetMapping("/accounts/{accountId}/conversations/unread-count")
    public ApiResponse<Map<String, Integer>> unreadCount(@PathVariable Long accountId) {
        int count = conversationMapper.countUnreadByAccountId(accountId);
        Map<String, Integer> result = new HashMap<>();
        result.put("unreadCount", count);
        return ApiResponse.ok(result);
    }

    @PostMapping("/conversations/{id}/read")
    public ApiResponse<Void> markConversationRead(@PathVariable Long id) {
        conversationMapper.markAsRead(id);
        return ApiResponse.ok(null);
    }

    // ==================== 消息管理 ====================

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<XianyuMessage>> listMessages(@PathVariable Long conversationId) {
        return ApiResponse.ok(messageMapper.selectByConversationId(conversationId));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResponse<XianyuMessage> sendMessage(@PathVariable Long conversationId, @RequestBody XianyuMessage message) {
        message.setConversationId(conversationId);
        message.setDirection("out");
        message.setSentAt(LocalDateTime.now());
        messageMapper.insert(message);

        // 更新会话的最后一条消息
        XianyuConversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            conversationMapper.updateNewMessage(conversationId, message.getContent(), LocalDateTime.now());
        }
        return ApiResponse.ok(message);
    }

    // ==================== 快捷回复 ====================

    @GetMapping("/quick-replies")
    public ApiResponse<List<XianyuQuickReply>> listQuickReplies() {
        long tenantId = TenantContext.requireTenantId();
        return ApiResponse.ok(quickReplyMapper.selectByTenantId(tenantId));
    }

    @PostMapping("/quick-replies")
    public ApiResponse<XianyuQuickReply> createQuickReply(@RequestBody XianyuQuickReply reply) {
        long tenantId = TenantContext.requireTenantId();
        reply.setTenantId(tenantId);
        quickReplyMapper.insert(reply);
        return ApiResponse.ok(reply);
    }

    @PutMapping("/quick-replies/{id}")
    public ApiResponse<XianyuQuickReply> updateQuickReply(@PathVariable Long id, @RequestBody XianyuQuickReply reply) {
        reply.setId(id);
        quickReplyMapper.update(reply);
        return ApiResponse.ok(quickReplyMapper.selectById(id));
    }

    @DeleteMapping("/quick-replies/{id}")
    public ApiResponse<Void> deleteQuickReply(@PathVariable Long id) {
        quickReplyMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    // ==================== 看板统计 ====================

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        long tenantId = TenantContext.requireTenantId();
        List<XianyuAccount> accounts = accountMapper.selectByTenantId(tenantId);

        int totalAccounts = accounts.size();
        int onlineAccounts = 0;
        int totalUnread = 0;

        for (XianyuAccount acc : accounts) {
            if ("online".equals(acc.getStatus())) {
                onlineAccounts++;
            }
            totalUnread += conversationMapper.countUnreadByAccountId(acc.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalAccounts", totalAccounts);
        result.put("onlineAccounts", onlineAccounts);
        result.put("totalUnread", totalUnread);
        return ApiResponse.ok(result);
    }
}
