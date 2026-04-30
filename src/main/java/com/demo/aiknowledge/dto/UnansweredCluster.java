package com.demo.aiknowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class UnansweredCluster {
    private String topic;
    private String topicSummary;
    private Integer totalCount;
    private List<String> questions;
    private List<String> suggestedKeywords;
}