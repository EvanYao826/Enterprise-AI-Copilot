package com.demo.aiknowledge.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String phone;
    private String code;
    private String password;
    private String username;
}
