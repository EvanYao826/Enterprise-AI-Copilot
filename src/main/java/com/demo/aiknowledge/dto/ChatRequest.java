package com.demo.aiknowledge.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long userId;
    private Long conversationId;
    private String content;
}
