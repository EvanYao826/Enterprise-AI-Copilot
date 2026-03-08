package com.demo.aiknowledge.interceptor;

import com.demo.aiknowledge.common.ErrorCode;
import com.demo.aiknowledge.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 简单模拟：检查 Header 中是否包含 X-Admin-Token
        // 实际生产环境应校验 JWT Token 的有效性及角色权限
        String token = request.getHeader("X-Admin-Token");
        
        // 这里简单约定：token 必须以 "admin_" 开头才算通过
        // 登录接口返回的 token 可以设计为 admin_ + userId
        if (token == null || !token.startsWith("admin_")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无权访问：非管理员请求");
        }
        
        return true;
    }
}
