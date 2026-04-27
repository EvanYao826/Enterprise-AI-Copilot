package com.demo.aiknowledge.dto;

import lombok.Data;

@Data
public class AgentRunRequest {
    private String conversationId;
    private String userId;
    private String input;
    private String agentType; // 可选，如 "knowledge_qa", "admin_copilot" 等
    private String runId; // 可选，如果不提供则自动生成
}
