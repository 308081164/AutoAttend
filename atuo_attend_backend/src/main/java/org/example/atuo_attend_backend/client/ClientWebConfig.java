package org.example.atuo_attend_backend.client;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ClientWebConfig {

    @Bean
    public ClientShellEnforcementFilter clientShellEnforcementFilter(ClientVersionPolicyService policyService) {
        return new ClientShellEnforcementFilter(policyService);
    }

    @Bean
    public FilterRegistrationBean<ClientShellEnforcementFilter> clientShellEnforcementFilterRegistration(
            ClientShellEnforcementFilter filter
    ) {
        FilterRegistrationBean<ClientShellEnforcementFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        return reg;
    }
}
