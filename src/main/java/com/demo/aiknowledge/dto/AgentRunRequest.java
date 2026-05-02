package com.demo.aiknowledge.dto;

import lombok.Data;

@Data
public class AgentRunRequest {
    private String runId;
    private String traceId;
    private String conversationId;
    private String userId;
    private String input;
    private String goal;
    private String agentType;
    private String context;
    private Boolean isAdmin;
}