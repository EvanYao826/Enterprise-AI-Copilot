package com.demo.aiknowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ToolCallDTO {
    private String id;
    private String toolCallId;
    private String runId;
    private String toolName;
    private String inputParams;
    private String output;
    private String status;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
}
