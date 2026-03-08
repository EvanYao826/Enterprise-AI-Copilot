package com.demo.aiknowledge.config;

import com.demo.aiknowledge.interceptor.AdminInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册管理员拦截器
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**") // 拦截所有 /api/admin/ 下的请求
                .excludePathPatterns("/api/admin/login"); // 排除登录接口
    }
}
