package com.demo.aiknowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护，对于前后端分离的应用
            .csrf(csrf -> csrf.disable())
            // 启用CORS支持
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 允许OPTIONS预检请求
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
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

        // 配置无状态会话管理
        http.sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000")); // 前端开发服务器
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization")); // 允许前端访问Authorization header

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
