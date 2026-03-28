package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiknowledge.common.ErrorCode;
import com.demo.aiknowledge.common.JwtUtil;
import com.demo.aiknowledge.entity.Admin;
import com.demo.aiknowledge.exception.BusinessException;
import com.demo.aiknowledge.mapper.AdminMapper;
import com.demo.aiknowledge.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;

    @Override
    public Map<String, Object> login(String username, String password) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username));
        
        if (admin == null || !admin.getPassword().equals(password)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "用户名或密码错误");
        }
        
        // 使用JWT生成token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", admin.getId());
        claims.put("username", admin.getUsername());
        claims.put("role", "ADMIN");

        String accessToken = jwtUtil.generateToken(admin.getId().toString(), claims);
        String refreshToken = jwtUtil.generateRefreshToken(admin.getId().toString());
        
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("admin", admin);
        
        return result;
    }
}
