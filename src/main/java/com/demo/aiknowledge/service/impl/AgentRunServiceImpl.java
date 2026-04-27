package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.aiknowledge.dto.AgentRunResponse;
import com.demo.aiknowledge.entity.AgentRun;
import com.demo.aiknowledge.mapper.AgentRunMapper;
import com.demo.aiknowledge.service.AgentRunService;
import com.demo.aiknowledge.service.ToolCallService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class AgentRunServiceImpl implements AgentRunService {
    
    @Resource
    private AgentRunMapper agentRunMapper;
    
    @Resource
    private ToolCallService toolCallService;
    
    @Override
    public void saveAgentRun(AgentRun agentRun) {
        agentRunMapper.insert(agentRun);
    }
    
    @Override
    public AgentRun getAgentRunById(String id) {
        return agentRunMapper.selectById(id);
    }
    
    @Override
    public AgentRun getAgentRunByRunId(String runId) {
        return agentRunMapper.selectByRunId(runId);
    }
    
    @Override
    public AgentRunResponse getAgentRunResponseById(String id) {
        AgentRun agentRun = getAgentRunById(id);
        if (agentRun == null) {
            return null;
        }
        
        AgentRunResponse response = new AgentRunResponse();
        response.setId(agentRun.getId());
        response.setRunId(agentRun.getRunId());
        response.setConversationId(agentRun.getConversationId());
        response.setUserId(agentRun.getUserId());
        response.setStatus(agentRun.getStatus());
        response.setStartTime(agentRun.getStartTime());
        response.setEndTime(agentRun.getEndTime());
        response.setInput(agentRun.getInput());
        response.setOutput(agentRun.getOutput());
        response.setErrorMessage(agentRun.getErrorMessage());
        response.setCreatedAt(agentRun.getCreatedAt());
        
        return response;
    }
    
    @Override
    public List<AgentRun> getAgentRunsByConversationId(String conversationId) {
        return agentRunMapper.selectByConversationId(conversationId);
    }
    
    @Override
    public List<AgentRun> getAgentRunsByUserId(String userId) {
        return agentRunMapper.selectByUserId(userId);
    }
    
    @Override
    public List<AgentRun> getAgentRunsByStatus(String status) {
        return agentRunMapper.selectByStatus(status);
    }
    
    @Override
    public List<AgentRun> getAllAgentRuns(int page, int size) {
        Page<AgentRun> agentRunPage = new Page<>(page, size);
        QueryWrapper<AgentRun> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("start_time");
        return agentRunMapper.selectPage(agentRunPage, wrapper).getRecords();
    }
    
    @Override
    public long countAgentRuns() {
        return agentRunMapper.selectCount(null);
    }
    
    @Override
    public boolean updateAgentRunStatus(String runId, String status) {
        AgentRun agentRun = getAgentRunByRunId(runId);
        if (agentRun != null) {
            agentRun.setStatus(status);
            agentRunMapper.updateById(agentRun);
            return true;
        }
        return false;
    }
    
    @Override
    public AgentRunResponse getAgentRunWithSteps(String runId) {
        // 这里应该查询步骤信息，暂时返回基本信息
        AgentRun agentRun = getAgentRunByRunId(runId);
        if (agentRun == null) {
            return null;
        }
        
        AgentRunResponse response = new AgentRunResponse();
        response.setId(agentRun.getId());
        response.setRunId(agentRun.getRunId());
        response.setConversationId(agentRun.getConversationId());
        response.setUserId(agentRun.getUserId());
        response.setStatus(agentRun.getStatus());
        response.setStartTime(agentRun.getStartTime());
        response.setEndTime(agentRun.getEndTime());
        response.setInput(agentRun.getInput());
        response.setOutput(agentRun.getOutput());
        response.setErrorMessage(agentRun.getErrorMessage());
        response.setCreatedAt(agentRun.getCreatedAt());
        
        return response;
    }
    
    @Override
    public AgentRunResponse getAgentRunWithStepsAndToolCalls(String runId) {
        // 这里应该查询步骤和工具调用信息，暂时返回基本信息
        return getAgentRunWithSteps(runId);
    }
}
