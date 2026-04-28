package com.demo.aiknowledge.dto;

import lombok.Data;

@Data
public class AgentRunRequest {
    private String runId; // 可选，如果不提供则自动生成
    private String traceId; // 可选，用于链路追踪
    private String conversationId;
    private String userId;
    private String input;
    private String goal; // 当前目标
    private String agentType; // 可选，如 "knowledge_qa", "admin_copilot" 等
    private String context; // 对话上下文
}
