package com.demo.aiknowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_summary")
public class DocSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId;
    private String summary;
    private LocalDateTime createTime;
}
