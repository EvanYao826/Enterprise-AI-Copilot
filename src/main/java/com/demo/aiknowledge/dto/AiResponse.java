package com.demo.aiknowledge.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiResponse {
    private String answer;
    private List<Map<String, Object>> sources;
    private String taskType; // 任务类型
}
