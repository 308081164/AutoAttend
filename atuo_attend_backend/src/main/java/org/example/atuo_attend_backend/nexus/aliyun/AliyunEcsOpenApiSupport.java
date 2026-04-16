package org.example.atuo_attend_backend.nexus.aliyun;

import com.aliyun.tea.TeaException;

/**
 * 将阿里云 ECS Tea SDK 异常转换为用户可理解的提示（尤其 RAM 权限不足）。
 */
public final class AliyunEcsOpenApiSupport {

    private AliyunEcsOpenApiSupport() {
    }

    /**
     * 业务错误码：与 RAM/阿里云拒绝或资源不存在相关时返回非 50000，便于前端区分展示。
     */
    public static int mapBusinessCode(TeaException ex) {
        if (ex == null) {
            return 50000;
        }
        String c = safeUpper(ex.getCode());
        Integer http = ex.getStatusCode();
        if (http != null && http == 403) {
            return 40302;
        }
        if (c.contains("FORBIDDEN")
                || c.contains("NOPermission")
                || c.contains("NO_PERMISSION")
                || c.contains("RAM")
                || c.contains("ACCESSDENIED")
                || c.contains("ACCESS_DENIED")) {
            return 40302;
        }
        if (c.contains("NOTFOUND") || c.contains("NOT_FOUND")
                || (c.contains("INVALID") && c.contains("ID"))) {
            return 40400;
        }
        return 50000;
    }

    /**
     * 中文说明 + 保留阿里云原始 code/message 便于排查。
     */
    public static String friendlyMessage(TeaException ex) {
        return friendlyMessage(ex,
                "请在阿里云 RAM 中为该密钥关联的策略中增加对应 ECS 接口权限："
                        + "只读列举需 ecs:DescribeSecurityGroups、ecs:DescribeSecurityGroupAttribute；"
                        + "新增/修改/删除规则需 ecs:AuthorizeSecurityGroup、ecs:ModifySecurityGroupRule、ecs:RevokeSecurityGroup（入方向）及 ecs:AuthorizeSecurityGroupEgress、ecs:ModifySecurityGroupEgressRule、ecs:RevokeSecurityGroupEgress（出方向）。"
                        + "更新策略后可在本页重试，或在「快捷运维」云账号设置中更换具备权限的 AccessKey。",
                "阿里云返回资源不存在或 ID 无效，请刷新安全组列表后重试。",
                "调用阿里云安全组接口失败。");
    }

    /**
     * 通用 Tea 异常说明（非 ECS 安全组场景，如备案 Beian OpenAPI）。
     */
    public static String friendlyMessage(TeaException ex, String ramHint403, String hint404, String hintOther) {
        if (ex == null) {
            return "调用阿里云接口失败";
        }
        String rawCode = ex.getCode();
        String rawMsg = ex.getMessage();
        int biz = mapBusinessCode(ex);
        StringBuilder sb = new StringBuilder();
        if (biz == 40302) {
            sb.append("当前云账号的 AccessKey 未获得阿里云 RAM 授权执行该操作。");
            sb.append(ramHint403 != null && !ramHint403.isBlank()
                    ? ramHint403
                    : "请在 RAM 中为该密钥增加对应云产品接口权限后重试。");
        } else if (biz == 40400) {
            sb.append(hint404 != null && !hint404.isBlank()
                    ? hint404
                    : "阿里云返回资源不存在或参数无效。");
        } else {
            sb.append(hintOther != null && !hintOther.isBlank()
                    ? hintOther
                    : "调用阿里云 OpenAPI 失败。");
        }
        if (rawCode != null && !rawCode.isBlank()) {
            sb.append(" [阿里云错误码: ").append(rawCode).append("]");
        }
        if (rawMsg != null && !rawMsg.isBlank()) {
            sb.append(" ").append(rawMsg);
        }
        return sb.toString();
    }

    public static TeaException unwrapTea(Throwable t) {
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth++ < 12) {
            if (cur instanceof TeaException te) {
                return te;
            }
            cur = cur.getCause();
        }
        return null;
    }

    private static String safeUpper(String s) {
        return s == null ? "" : s.toUpperCase();
    }
}
