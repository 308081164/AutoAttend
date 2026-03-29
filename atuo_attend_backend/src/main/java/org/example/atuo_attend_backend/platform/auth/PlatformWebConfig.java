package org.example.atuo_attend_backend.platform.auth;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlatformWebConfig {

    @Bean
    public FilterRegistrationBean<PlatformAuthFilter> platformAuthFilterRegistration(PlatformAuthFilter filter) {
        FilterRegistrationBean<PlatformAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/api/platform/*");
        reg.setOrder(0);
        return reg;
    }
}
