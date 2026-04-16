-- 快捷运维扩展：DNS 域名/解析、OSS Bucket、短信签名与模板（缓存）、ICP 备案登记（手工）

CREATE TABLE IF NOT EXISTS aa_nexus_dns_domain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    domain_name VARCHAR(255) NOT NULL,
    remark VARCHAR(512) NULL,
    synced_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_nexus_dns_acc (tenant_id, account_id),
    UNIQUE KEY uk_nexus_dns_domain (tenant_id, account_id, domain_name(128))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aa_nexus_dns_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    domain_name VARCHAR(255) NOT NULL,
    aliyun_record_id VARCHAR(64) NOT NULL,
    rr VARCHAR(255) NOT NULL DEFAULT '',
    record_type VARCHAR(32) NOT NULL,
    record_value VARCHAR(1024) NOT NULL DEFAULT '',
    ttl INT NULL,
    line VARCHAR(64) NULL,
    synced_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_nexus_dnsrec_acc (tenant_id, account_id),
    KEY idx_nexus_dnsrec_dom (tenant_id, account_id, domain_name(128)),
    UNIQUE KEY uk_nexus_dnsrec (tenant_id, account_id, aliyun_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aa_nexus_oss_bucket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    bucket_name VARCHAR(255) NOT NULL,
    region VARCHAR(64) NULL,
    location VARCHAR(255) NULL,
    synced_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_nexus_oss_acc (tenant_id, account_id),
    UNIQUE KEY uk_nexus_oss_bucket (tenant_id, account_id, bucket_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aa_nexus_sms_signature (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    sign_name VARCHAR(64) NOT NULL,
    audit_status VARCHAR(32) NULL,
    sign_type VARCHAR(32) NULL,
    synced_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_nexus_sms_sign_acc (tenant_id, account_id),
    UNIQUE KEY uk_nexus_sms_sign (tenant_id, account_id, sign_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aa_nexus_sms_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NULL,
    template_type VARCHAR(32) NULL,
    audit_status VARCHAR(32) NULL,
    synced_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_nexus_sms_tpl_acc (tenant_id, account_id),
    UNIQUE KEY uk_nexus_sms_tpl (tenant_id, account_id, template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aa_nexus_icp_site (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    domain_name VARCHAR(255) NOT NULL,
    site_name VARCHAR(255) NULL,
    icp_license VARCHAR(128) NULL,
    status_text VARCHAR(255) NULL,
    remark VARCHAR(1024) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_nexus_icp_acc (tenant_id, account_id),
    KEY idx_nexus_icp_domain (tenant_id, account_id, domain_name(128))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
