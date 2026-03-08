package com.demo.aiknowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.aiknowledge.entity.User;

public interface UserService extends IService<User> {
    void updateStatus(Long userId, Integer status);
}
