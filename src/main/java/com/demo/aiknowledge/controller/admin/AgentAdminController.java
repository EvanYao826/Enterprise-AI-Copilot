package com.demo.aiknowledge.controller.admin;

import com.demo.aiknowledge.entity.AgentRun;
import com.demo.aiknowledge.entity.ToolCall;
import com.demo.aiknowledge.service.AgentRunService;
import com.demo.aiknowledge.service.ToolCallService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/admin/agent")
public class AgentAdminController {
    
    @Resource
    private AgentRunService agentRunService;
    
    @Resource
    private ToolCallService toolCallService;
    
    /**
     * 获取 Agent 运行记录列表
     */
    @GetMapping("/runs")
    public List<AgentRun> getAgentRuns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId
    ) {
        if (status != null) {
            return agentRunService.getAgentRunsByStatus(status);
        }
        if (userId != null) {
            return agentRunService.getAgentRunsByUserId(userId);
        }
        return agentRunService.getAllAgentRuns(page, size);
    }
    
    /**
     * 获取工具调用记录列表
     */
    @GetMapping("/tool-calls")
    public List<ToolCall> getToolCalls(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String toolName
    ) {
        // 这里可以根据状态和工具名称进行过滤
        return toolCallService.getAllToolCalls(page, size);
    }
    
    /**
     * 获取失败的工具调用记录
     */
    @GetMapping("/tool-calls/failed")
    public List<ToolCall> getFailedToolCalls(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return toolCallService.getFailedToolCalls(limit);
    }
}
