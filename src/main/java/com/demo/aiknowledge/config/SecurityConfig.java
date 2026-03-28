package com.demo.aiknowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

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
                // 管理员接口需要ADMIN角色
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 用户接口需要USER或ADMIN角色
                .requestMatchers("/api/chat/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/knowledge/**").hasAnyRole("USER", "ADMIN")
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            // 添加JWT过滤器
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // 禁用默认的登录表单
            .formLogin(form -> form.disable())
            // 禁用默认的HTTP基本认证
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}
