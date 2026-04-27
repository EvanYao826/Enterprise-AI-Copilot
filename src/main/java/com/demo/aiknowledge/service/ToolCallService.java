package com.demo.aiknowledge.service;

import com.demo.aiknowledge.entity.ToolCall;

import java.util.List;

public interface ToolCallService {
    void saveToolCall(ToolCall toolCall);
    List<ToolCall> getToolCallsByRunId(String runId);
    List<ToolCall> getFailedToolCalls(int limit);
    List<ToolCall> getAllToolCalls(int page, int size);
    long countToolCalls();
}
