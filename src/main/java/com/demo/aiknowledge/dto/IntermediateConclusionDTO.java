package com.demo.aiknowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class IntermediateConclusionDTO {
    private String stepId;
    private String conclusionType;
    private Object content;
    private Double confidence;
    private List<SourceDTO> sources;
}
