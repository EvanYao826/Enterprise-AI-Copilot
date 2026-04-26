package com.demo.aiknowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_doc")
public class KnowledgeDoc {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docName;
    private String filePath;
    private Long categoryId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String errorMessage; // 存储解析失败的错误原因
    private LocalDateTime createTime;
}
