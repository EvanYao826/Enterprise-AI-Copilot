package com.demo.aiknowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护，对于前后端分离的应用
            .csrf(csrf -> csrf.disable())
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 允许所有/api/auth下的请求
                .requestMatchers("/api/auth/**").permitAll()
                // 允许所有/api/admin/login请求
                .requestMatchers("/api/admin/login").permitAll()
                // 允许所有其他请求（根据实际需求调整）
                .anyRequest().permitAll()
            )
            // 禁用默认的登录表单
            .formLogin(form -> form.disable())
            // 禁用默认的HTTP基本认证
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}
