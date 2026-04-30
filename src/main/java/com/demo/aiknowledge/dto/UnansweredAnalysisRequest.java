package com.demo.aiknowledge.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UnansweredAnalysisRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer minCount = 1;
    private Integer clusterThreshold = 3;
}