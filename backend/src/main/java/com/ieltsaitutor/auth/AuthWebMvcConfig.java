package com.ieltsaitutor.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ieltsaitutor.rag.admin.AdminTokenInterceptor;

@Configuration
public class AuthWebMvcConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final AdminTokenInterceptor adminTokenInterceptor;

    public AuthWebMvcConfig(AuthInterceptor authInterceptor, AdminTokenInterceptor adminTokenInterceptor) {
        this.authInterceptor = authInterceptor;
        this.adminTokenInterceptor = adminTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(adminTokenInterceptor).addPathPatterns("/api/admin/rag/**");
    }
}
