package com.demo.aiknowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("qa_unanswered")
public class QaUnanswered {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String question;
    private Integer count;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
