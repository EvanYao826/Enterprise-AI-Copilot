package com.demo.aiknowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AgentRunResponse {
    private String id;
    private String runId;
    private String traceId;
    private String conversationId;
    private String userId;
    private String status;
    private String goal;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String input;
    private String output;
    private String errorMessage;
    private String errorCode;
    private LocalDateTime createdAt;
    private Integer currentStepIndex;
    private List<AgentStepDTO> steps;
    private List<ToolCallDTO> toolCalls;
    private List<IntermediateConclusionDTO> intermediateConclusions;
    private Long elapsedTime;
    private Integer maxSteps;
    private Integer timeoutSeconds;
}