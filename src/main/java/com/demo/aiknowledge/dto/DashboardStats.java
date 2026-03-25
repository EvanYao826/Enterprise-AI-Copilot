package com.demo.aiknowledge.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStats {
    private Long userCount;
    private Long docCount;
    private Long qaCount;
    private Double hitRate;
    
    private List<Map<String, Object>> hotDocs;
    private List<Map<String, Object>> topQuestions;
    private List<Map<String, Object>> unansweredQuestions;
    private List<Map<String, Object>> questionTrends;
}
