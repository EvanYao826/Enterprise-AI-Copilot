package com.demo.aiknowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QaUnansweredExport {
    private String question;
    private Integer count;
    private LocalDateTime firstOccurrence;
    private LocalDateTime lastOccurrence;
}