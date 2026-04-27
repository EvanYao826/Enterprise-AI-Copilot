package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.aiknowledge.entity.ToolCall;
import com.demo.aiknowledge.mapper.ToolCallMapper;
import com.demo.aiknowledge.service.ToolCallService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ToolCallServiceImpl implements ToolCallService {
    
    @Resource
    private ToolCallMapper toolCallMapper;
    
    @Override
    public void saveToolCall(ToolCall toolCall) {
        toolCall.setCreatedAt(LocalDateTime.now());
        toolCallMapper.insert(toolCall);
    }
    
    @Override
    public List<ToolCall> getToolCallsByRunId(String runId) {
        return toolCallMapper.selectByRunId(runId);
    }
    
    @Override
    public List<ToolCall> getFailedToolCalls(int limit) {
        return toolCallMapper.selectFailedToolCalls(limit);
    }
    
    @Override
    public List<ToolCall> getAllToolCalls(int page, int size) {
        Page<ToolCall> toolCallPage = new Page<>(page, size);
        QueryWrapper<ToolCall> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_at");
        return toolCallMapper.selectPage(toolCallPage, wrapper).getRecords();
    }
    
    @Override
    public long countToolCalls() {
        return toolCallMapper.selectCount(null);
    }
}
