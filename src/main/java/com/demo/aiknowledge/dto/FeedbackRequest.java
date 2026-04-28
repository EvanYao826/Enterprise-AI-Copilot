package com.demo.aiknowledge.dto;

import lombok.Data;

@Data
public class FeedbackRequest {
    private Long messageId;
    private String feedbackType; // like / dislike
}