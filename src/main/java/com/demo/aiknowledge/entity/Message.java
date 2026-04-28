package com.demo.aiknowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String role; // user or assistant
    private String content;
    private String sources; // JSON string
    private Double importanceScore; // 消息重要性评分（0-1）
    private String feedbackType; // 反馈类型（like/dislike）
    private LocalDateTime feedbackTime; // 反馈时间
    private LocalDateTime createTime;
}
