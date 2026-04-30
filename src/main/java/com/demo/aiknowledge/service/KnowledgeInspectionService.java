package com.demo.aiknowledge.service;

import com.demo.aiknowledge.dto.UnansweredAnalysisRequest;
import com.demo.aiknowledge.dto.UnansweredAnalysisResponse;

import java.util.Map;

public interface KnowledgeInspectionService {
    UnansweredAnalysisResponse analyzeUnansweredQuestions(UnansweredAnalysisRequest request);
    Map<String, Object> getUnansweredStatistics();
}