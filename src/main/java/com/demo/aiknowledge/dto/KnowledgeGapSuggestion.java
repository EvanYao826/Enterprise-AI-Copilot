package com.demo.aiknowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeGapSuggestion {
    private String topic;
    private String suggestionType;
    private String suggestion;
    private Integer questionCount;
    private String priority;
    private String relatedCategory;
    private List<String> suggestedKeywords;
}