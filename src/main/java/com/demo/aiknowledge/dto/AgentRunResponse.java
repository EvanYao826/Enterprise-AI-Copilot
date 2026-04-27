package com.demo.aiknowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AgentRunResponse {
    private String id;
    private String runId;
    private String conversationId;
    private String userId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String input;
    private String output;
    private String errorMessage;
    private LocalDateTime createdAt;
    private List<AgentStepDTO> steps;
    private List<ToolCallDTO> toolCalls;
}
