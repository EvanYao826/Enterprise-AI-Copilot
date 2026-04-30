package com.demo.aiknowledge.controller.admin;

import com.demo.aiknowledge.common.Result;
import com.demo.aiknowledge.dto.UnansweredAnalysisRequest;
import com.demo.aiknowledge.dto.UnansweredAnalysisResponse;
import com.demo.aiknowledge.service.KnowledgeInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge-inspection")
@RequiredArgsConstructor
public class KnowledgeInspectionController {

    private final KnowledgeInspectionService knowledgeInspectionService;

    @GetMapping("/unanswered/analyze")
    public Result<UnansweredAnalysisResponse> analyzeUnanswered(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer minCount,
            @RequestParam(defaultValue = "3") Integer clusterThreshold) {
        
        UnansweredAnalysisRequest request = new UnansweredAnalysisRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setMinCount(minCount);
        request.setClusterThreshold(clusterThreshold);
        
        UnansweredAnalysisResponse response = knowledgeInspectionService.analyzeUnansweredQuestions(request);
        return Result.success(response);
    }

    @GetMapping("/unanswered/statistics")
    public Result<Map<String, Object>> getUnansweredStatistics() {
        Map<String, Object> stats = knowledgeInspectionService.getUnansweredStatistics();
        return Result.success(stats);
    }

    @PostMapping("/run")
    public Result<UnansweredAnalysisResponse> runInspection(@RequestBody(required = false) UnansweredAnalysisRequest request) {
        if (request == null) {
            request = new UnansweredAnalysisRequest();
        }
        UnansweredAnalysisResponse response = knowledgeInspectionService.analyzeUnansweredQuestions(request);
        return Result.success(response);
    }
}