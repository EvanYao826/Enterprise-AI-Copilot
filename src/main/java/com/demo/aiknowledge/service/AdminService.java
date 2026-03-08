package com.demo.aiknowledge.service;

import com.demo.aiknowledge.entity.Admin;
import java.util.Map;

public interface AdminService {
    Map<String, Object> login(String username, String password);
}
