package com.demo.aiknowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_run")
public class AgentRun {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String runId;
    private String traceId;
    private String conversationId;
    private String userId;
    private String status;
    private String goal;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String input;
    private String output;
    private String errorMessage;
    private String errorCode;
    private LocalDateTime createdAt;
}
