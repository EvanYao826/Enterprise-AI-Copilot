package com.demo.aiknowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tool_call")
public class ToolCall {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String toolCallId;
    private String runId;
    private String toolName;
    private String inputParams;
    private String output;
    private String status;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
}
