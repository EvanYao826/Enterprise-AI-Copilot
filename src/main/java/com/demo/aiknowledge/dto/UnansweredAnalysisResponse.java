package com.demo.aiknowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class UnansweredAnalysisResponse {
    private Integer totalUnansweredCount;
    private Integer totalUniqueQuestions;
    private Integer clusterCount;
    private List<UnansweredCluster> clusters;
    private List<KnowledgeGapSuggestion> suggestions;
    private List<QaUnansweredExport> exportData;
}