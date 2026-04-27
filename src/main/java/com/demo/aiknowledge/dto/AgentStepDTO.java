package com.demo.aiknowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentStepDTO {
    private String id;
    private String runId;
    private String stepName;
    private String status;
    private String input;
    private String output;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
}
