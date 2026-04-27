package com.demo.aiknowledge.service;

import com.demo.aiknowledge.dto.AgentRunResponse;
import com.demo.aiknowledge.entity.AgentRun;

import java.util.List;

public interface AgentRunService {
    void saveAgentRun(AgentRun agentRun);
    AgentRun getAgentRunById(String id);
    AgentRun getAgentRunByRunId(String runId);
    AgentRunResponse getAgentRunResponseById(String id);
    List<AgentRun> getAgentRunsByConversationId(String conversationId);
    List<AgentRun> getAgentRunsByUserId(String userId);
    List<AgentRun> getAgentRunsByStatus(String status);
    List<AgentRun> getAllAgentRuns(int page, int size);
    long countAgentRuns();
    boolean updateAgentRunStatus(String runId, String status);
    AgentRunResponse getAgentRunWithSteps(String runId);
    AgentRunResponse getAgentRunWithStepsAndToolCalls(String runId);
}
