package org.example.atuo_attend_backend.collab.config;

import org.example.atuo_attend_backend.collab.auth.CollabAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CollabWebConfig {

    @Bean
    public FilterRegistrationBean<CollabAuthFilter> collabAuthFilter(CollabAuthFilter filter) {
        FilterRegistrationBean<CollabAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/api/collab/*");
        reg.setOrder(1);
        return reg;
    }
}
