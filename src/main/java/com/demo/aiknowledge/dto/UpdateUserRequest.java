package com.demo.aiknowledge.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private Long userId;
    private String username;
    private String password;
}
