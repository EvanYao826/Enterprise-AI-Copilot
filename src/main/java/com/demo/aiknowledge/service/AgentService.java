package com.demo.aiknowledge.service;

import com.demo.aiknowledge.dto.AgentRunRequest;
import com.demo.aiknowledge.dto.AgentRunResponse;

public interface AgentService {
    /**
     * 运行 Agent
     */
    AgentRunResponse runAgent(AgentRunRequest request);
    
    /**
     * 取消 Agent 运行
     */
    boolean cancelAgentRun(String runId);
    
    /**
     * 重试 Agent 运行
     */
    AgentRunResponse retryAgentRun(String runId);
}
