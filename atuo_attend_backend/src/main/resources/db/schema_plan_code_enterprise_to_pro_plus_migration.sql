-- 历史 enterprise 档位统一为 pro_plus（专业增强版）
UPDATE aa_tenant SET plan_code = 'pro_plus' WHERE plan_code = 'enterprise';
UPDATE aa_tenant SET billing_baseline_plan_code = 'pro_plus' WHERE billing_baseline_plan_code = 'enterprise';
