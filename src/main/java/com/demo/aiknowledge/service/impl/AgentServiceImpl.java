package com.demo.aiknowledge.service.impl;

import com.demo.aiknowledge.dto.AgentRunRequest;
import com.demo.aiknowledge.dto.AgentRunResponse;
import com.demo.aiknowledge.entity.AgentRun;
import com.demo.aiknowledge.service.AgentService;
import com.demo.aiknowledge.service.AgentRunService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AgentServiceImpl implements AgentService {
    
    @Resource
    private AgentRunService agentRunService;
    
    @Override
    public AgentRunResponse runAgent(AgentRunRequest request) {
        // 生成 runId（如果没有提供）
        String runId = request.getRunId() != null ? request.getRunId() : UUID.randomUUID().toString();
        
        // 创建 AgentRun 记录
        AgentRun agentRun = new AgentRun();
        agentRun.setId(UUID.randomUUID().toString());
        agentRun.setRunId(runId);
        agentRun.setConversationId(request.getConversationId());
        agentRun.setUserId(request.getUserId());
        agentRun.setStatus("running");
        agentRun.setStartTime(LocalDateTime.now());
        agentRun.setInput(request.getInput());
        agentRun.setCreatedAt(LocalDateTime.now());
        
        // 保存到数据库
        agentRunService.saveAgentRun(agentRun);
        
        // 这里应该调用 Python 服务的 Agent 执行接口
        // 暂时返回一个模拟响应
        AgentRunResponse response = new AgentRunResponse();
        response.setId(agentRun.getId());
        response.setRunId(agentRun.getRunId());
        response.setConversationId(agentRun.getConversationId());
        response.setUserId(agentRun.getUserId());
        response.setStatus(agentRun.getStatus());
        response.setStartTime(agentRun.getStartTime());
        response.setInput(agentRun.getInput());
        response.setCreatedAt(agentRun.getCreatedAt());
        
        return response;
    }
    
    @Override
    public boolean cancelAgentRun(String runId) {
        // 实现取消逻辑
        return agentRunService.updateAgentRunStatus(runId, "cancelled");
    }
    
    @Override
    public AgentRunResponse retryAgentRun(String runId) {
        // 获取原运行记录
        AgentRun originalRun = agentRunService.getAgentRunByRunId(runId);
        if (originalRun == null) {
            return null;
        }
        
        // 创建新的运行记录
        AgentRun newRun = new AgentRun();
        newRun.setId(UUID.randomUUID().toString());
        newRun.setRunId(UUID.randomUUID().toString());
        newRun.setConversationId(originalRun.getConversationId());
        newRun.setUserId(originalRun.getUserId());
        newRun.setStatus("running");
        newRun.setStartTime(LocalDateTime.now());
        newRun.setInput(originalRun.getInput());
        newRun.setCreatedAt(LocalDateTime.now());
        
        // 保存到数据库
        agentRunService.saveAgentRun(newRun);
        
        // 这里应该调用 Python 服务的 Agent 执行接口
        // 暂时返回一个模拟响应
        AgentRunResponse response = new AgentRunResponse();
        response.setId(newRun.getId());
        response.setRunId(newRun.getRunId());
        response.setConversationId(newRun.getConversationId());
        response.setUserId(newRun.getUserId());
        response.setStatus(newRun.getStatus());
        response.setStartTime(newRun.getStartTime());
        response.setInput(newRun.getInput());
        response.setCreatedAt(newRun.getCreatedAt());
        
        return response;
    }
}
