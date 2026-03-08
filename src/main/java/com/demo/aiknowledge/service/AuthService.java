package com.demo.aiknowledge.service;

import com.demo.aiknowledge.entity.User;

public interface AuthService {
    void sendSmsCode(String phone);
    User register(String phone, String code, String password, String username);
    User login(String phone, String password);
    User updateUserInfo(Long userId, String username, String password);
}
