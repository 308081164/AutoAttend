package org.example.atuo_attend_backend.tenant.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class TenantWebConfig {

    @Bean
    public FilterRegistrationBean<TenantClearFilter> tenantClearFilterRegistration() {
        FilterRegistrationBean<TenantClearFilter> reg = new FilterRegistrationBean<>(new TenantClearFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }
}
