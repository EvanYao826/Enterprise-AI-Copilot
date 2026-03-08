package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiknowledge.common.ErrorCode;
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

    @Override
    public Map<String, Object> login(String username, String password) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username));
        
        if (admin == null || !admin.getPassword().equals(password)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "用户名或密码错误");
        }
        
        // 简单生成 Token (实际生产环境应使用 JWT)
        String token = "admin_" + admin.getId() + "_" + System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("admin", admin);
        
        return result;
    }
}
