package com.demo.aiknowledge.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.aiknowledge.common.Result;
import com.demo.aiknowledge.entity.AgentRun;
import com.demo.aiknowledge.entity.AgentStep;
import com.demo.aiknowledge.mapper.AgentRunMapper;
import com.demo.aiknowledge.mapper.AgentStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/agent-run")
@RequiredArgsConstructor
@Slf4j
public class AgentRunController {

    private final AgentRunMapper agentRunMapper;
    private final AgentStepMapper agentStepMapper;

    @GetMapping("/list")
    public Result<IPage<AgentRun>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {

        Page<AgentRun> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AgentRun> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(AgentRun::getUserId, userId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AgentRun::getStatus, status);
        }
        if (startTime != null) {
            wrapper.ge(AgentRun::getStartTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AgentRun::getStartTime, endTime);
        }

        wrapper.orderByDesc(AgentRun::getStartTime);
        IPage<AgentRun> result = agentRunMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @GetMapping("/{runId}")
    public Result<AgentRun> getById(@PathVariable String runId) {
        AgentRun agentRun = agentRunMapper.selectOne(
                new LambdaQueryWrapper<AgentRun>().eq(AgentRun::getRunId, runId));
        if (agentRun == null) {
            return Result.error("AgentRun不存在");
        }
        return Result.success(agentRun);
    }

    @GetMapping("/{runId}/steps")
    public Result<List<AgentStep>> getSteps(@PathVariable String runId) {
        List<AgentStep> steps = agentStepMapper.selectList(
                new LambdaQueryWrapper<AgentStep>()
                        .eq(AgentStep::getRunId, runId)
                        .orderByAsc(AgentStep::getCreatedAt));
        return Result.success(steps);
    }

    @GetMapping("/{runId}/tool-calls")
    public Result<List<?>> getToolCalls(@PathVariable String runId) {
        return Result.success(List.of());
    }

    @DeleteMapping("/{runId}")
    public Result<Void> delete(@PathVariable String runId) {
        int deleted = agentRunMapper.delete(
                new LambdaQueryWrapper<AgentRun>().eq(AgentRun::getRunId, runId));
        if (deleted > 0) {
            return Result.success(null);
        }
        return Result.error("删除失败");
    }
}
