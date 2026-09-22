package com.ieltsaitutor.rag.admin;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RagAdminWebMvcConfig implements WebMvcConfigurer {
    private final AdminTokenInterceptor interceptor;

    public RagAdminWebMvcConfig(AdminAuthorizationService authorization) {
        this.interceptor = new AdminTokenInterceptor(authorization);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/admin/rag/**");
    }
}
