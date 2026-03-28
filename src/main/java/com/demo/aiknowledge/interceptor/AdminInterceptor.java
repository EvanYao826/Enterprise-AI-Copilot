package com.demo.aiknowledge.interceptor;

import com.demo.aiknowledge.common.ErrorCode;
import com.demo.aiknowledge.common.JwtUtil;
import com.demo.aiknowledge.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "无权访问：缺少有效的管理员token");
        }
        
        token = token.substring(7);
        
        try {
            if (jwtUtil.validateToken(token)) {
                Map<String, Object> claims = jwtUtil.parseToken(token);
                String role = (String) claims.get("role");
                
                if (!"ADMIN".equals(role)) {
                    // 修复：使用具体的错误码枚举，或复用 INVALID_TOKEN
                    throw new BusinessException(ErrorCode.INVALID_TOKEN, "无权访问：非管理员请求");
                }
            } else {
                // 修复：直接传入 ErrorCode 枚举，去掉 .getCode()
                throw new BusinessException(ErrorCode.INVALID_TOKEN, "无权访问：无效的 token");
            }
        } catch (BusinessException e) {
            // 如果是业务异常直接抛出，避免重复包装
            throw e;
        } catch (Exception e) {
            // 修复：直接传入 ErrorCode 枚举
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "无权访问：token 验证失败");
        }
        
        return true;
    }
}
