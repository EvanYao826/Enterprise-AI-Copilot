package com.demo.aiknowledge.controller;

import com.demo.aiknowledge.dto.AgentRunRequest;
import com.demo.aiknowledge.dto.AgentRunResponse;
import com.demo.aiknowledge.service.AgentService;
import com.demo.aiknowledge.service.AgentRunService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/agent")
public class AgentController {
    
    @Resource
    private AgentService agentService;
    
    @Resource
    private AgentRunService agentRunService;
    
    /**
     * 启动 Agent 运行
     */
    @PostMapping("/run")
    public AgentRunResponse runAgent(@RequestBody AgentRunRequest request) {
        return agentService.runAgent(request);
    }
    
    /**
     * 获取 Agent 运行详情
     */
    @GetMapping("/run/{id}")
    public AgentRunResponse getAgentRun(@PathVariable String id) {
        return agentRunService.getAgentRunResponseById(id);
    }
    
    /**
     * 获取 Agent 运行的步骤
     */
    @GetMapping("/run/{id}/steps")
    public AgentRunResponse getAgentRunSteps(@PathVariable String id) {
        return agentRunService.getAgentRunWithSteps(id);
    }
}
