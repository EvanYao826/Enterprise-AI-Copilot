package com.demo.aiknowledge.service.impl;

import com.demo.aiknowledge.dto.*;
import com.demo.aiknowledge.entity.QaUnanswered;
import com.demo.aiknowledge.entity.KnowledgeCategory;
import com.demo.aiknowledge.mapper.QaUnansweredMapper;
import com.demo.aiknowledge.mapper.KnowledgeCategoryMapper;
import com.demo.aiknowledge.service.KnowledgeInspectionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class KnowledgeInspectionServiceImpl implements KnowledgeInspectionService {

    @Resource
    private QaUnansweredMapper qaUnansweredMapper;

    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}|[a-zA-Z]+");

    @Override
    public UnansweredAnalysisResponse analyzeUnansweredQuestions(UnansweredAnalysisRequest request) {
        List<QaUnanswered> allRecords = qaUnansweredMapper.selectAll();
        
        if (request.getStartDate() != null || request.getEndDate() != null) {
            LocalDateTime start = request.getStartDate() != null ? 
                request.getStartDate().atStartOfDay() : null;
            LocalDateTime end = request.getEndDate() != null ? 
                request.getEndDate().atTime(LocalTime.MAX) : null;
            allRecords = filterByTimeRange(allRecords, start, end);
        }

        if (request.getMinCount() != null && request.getMinCount() > 1) {
            allRecords = allRecords.stream()
                .filter(r -> r.getCount() >= request.getMinCount())
                .collect(Collectors.toList());
        }

        List<UnansweredCluster> clusters = clusterQuestions(allRecords, request.getClusterThreshold());
        List<KnowledgeGapSuggestion> suggestions = generateSuggestions(clusters);
        List<QaUnansweredExport> exportData = convertToExportData(allRecords);

        UnansweredAnalysisResponse response = new UnansweredAnalysisResponse();
        response.setTotalUnansweredCount(allRecords.stream().mapToInt(QaUnanswered::getCount).sum());
        response.setTotalUniqueQuestions(allRecords.size());
        response.setClusterCount(clusters.size());
        response.setClusters(clusters);
        response.setSuggestions(suggestions);
        response.setExportData(exportData);

        return response;
    }

    @Override
    public Map<String, Object> getUnansweredStatistics() {
        List<QaUnanswered> allRecords = qaUnansweredMapper.selectAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUnansweredCount", allRecords.stream().mapToInt(QaUnanswered::getCount).sum());
        stats.put("totalUniqueQuestions", allRecords.size());
        stats.put("topQuestions", allRecords.stream()
            .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
            .limit(10)
            .map(r -> Map.of("question", r.getQuestion(), "count", r.getCount()))
            .collect(Collectors.toList()));
        
        return stats;
    }

    private List<QaUnanswered> filterByTimeRange(List<QaUnanswered> records, LocalDateTime start, LocalDateTime end) {
        return records.stream().filter(r -> {
            LocalDateTime createTime = r.getCreateTime();
            return (start == null || !createTime.isBefore(start)) && (end == null || !createTime.isAfter(end));
        }).collect(Collectors.toList());
    }

    private List<UnansweredCluster> clusterQuestions(List<QaUnanswered> records, int threshold) {
        if (records.isEmpty()) return Collections.emptyList();

        Map<String, List<QaUnanswered>> clusters = new HashMap<>();
        Set<QaUnanswered> processed = new HashSet<>();

        for (QaUnanswered record : records) {
            if (processed.contains(record)) continue;

            String seedQuestion = record.getQuestion();
            
            List<QaUnanswered> cluster = new ArrayList<>();
            cluster.add(record);
            processed.add(record);

            for (QaUnanswered other : records) {
                if (processed.contains(other)) continue;
                if (isSimilar(seedQuestion, other.getQuestion(), threshold)) {
                    cluster.add(other);
                    processed.add(other);
                }
            }

            if (!cluster.isEmpty()) {
                String topic = generateClusterTopic(cluster);
                clusters.put(topic, cluster);
            }
        }

        return clusters.entrySet().stream()
            .map(entry -> {
                UnansweredCluster cluster = new UnansweredCluster();
                cluster.setTopic(entry.getKey());
                cluster.setTopicSummary(summarizeTopic(entry.getValue()));
                cluster.setTotalCount(entry.getValue().stream().mapToInt(QaUnanswered::getCount).sum());
                cluster.setQuestions(entry.getValue().stream()
                    .map(QaUnanswered::getQuestion)
                    .collect(Collectors.toList()));
                cluster.setSuggestedKeywords(extractKeywords(entry.getValue()));
                return cluster;
            })
            .sorted((a, b) -> Integer.compare(b.getTotalCount(), a.getTotalCount()))
            .collect(Collectors.toList());
    }

    private boolean isSimilar(String q1, String q2, int threshold) {
        String clean1 = cleanQuestion(q1);
        String clean2 = cleanQuestion(q2);
        
        int commonChars = countCommonCharacters(clean1, clean2);
        int maxLen = Math.max(clean1.length(), clean2.length());
        
        return maxLen > 0 && (double) commonChars / maxLen >= (threshold / 10.0);
    }

    private String cleanQuestion(String question) {
        if (question == null) return "";
        return question.toLowerCase()
            .replaceAll("[\\s\\p{Punct}]", "")
            .replaceAll("[a-zA-Z0-9]", "");
    }

    private int countCommonCharacters(String s1, String s2) {
        Set<Character> chars1 = s1.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        return (int) s2.chars().filter(c -> chars1.contains((char) c)).count();
    }

    private String extractTopic(String question) {
        if (question == null || question.isEmpty()) return "其他";
        
        Matcher matcher = CHINESE_PATTERN.matcher(question);
        List<String> chineseParts = new ArrayList<>();
        while (matcher.find()) {
            chineseParts.add(matcher.group());
        }
        
        if (chineseParts.isEmpty()) return "其他";
        
        String longest = chineseParts.stream()
            .max(Comparator.comparingInt(String::length))
            .orElse("其他");
        
        return longest.length() > 4 ? longest.substring(0, 4) : longest;
    }

    private String generateClusterTopic(List<QaUnanswered> cluster) {
        Map<String, Integer> wordCount = new HashMap<>();
        
        for (QaUnanswered record : cluster) {
            Matcher matcher = KEYWORD_PATTERN.matcher(record.getQuestion());
            while (matcher.find()) {
                String word = matcher.group();
                wordCount.put(word, wordCount.getOrDefault(word, 0) + record.getCount());
            }
        }
        
        return wordCount.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.joining("、"));
    }

    private String summarizeTopic(List<QaUnanswered> cluster) {
        if (cluster.isEmpty()) return "";
        
        QaUnanswered top = cluster.stream()
            .max(Comparator.comparingInt(QaUnanswered::getCount))
            .orElse(cluster.get(0));
        
        String question = top.getQuestion();
        if (question.length() <= 20) return question;
        return question.substring(0, 20) + "...";
    }

    private List<String> extractKeywords(List<QaUnanswered> cluster) {
        Map<String, Integer> wordCount = new HashMap<>();
        
        for (QaUnanswered record : cluster) {
            Matcher matcher = KEYWORD_PATTERN.matcher(record.getQuestion());
            while (matcher.find()) {
                String word = matcher.group();
                if (word.length() >= 2) {
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + record.getCount());
                }
            }
        }
        
        return wordCount.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private List<KnowledgeGapSuggestion> generateSuggestions(List<UnansweredCluster> clusters) {
        List<KnowledgeCategory> categories = knowledgeCategoryMapper.selectList(null);
        List<String> existingCategories = categories.stream()
            .map(KnowledgeCategory::getName)
            .collect(Collectors.toList());

        List<KnowledgeGapSuggestion> suggestions = new ArrayList<>();

        for (UnansweredCluster cluster : clusters) {
            KnowledgeGapSuggestion suggestion = new KnowledgeGapSuggestion();
            suggestion.setTopic(cluster.getTopic());
            suggestion.setQuestionCount(cluster.getTotalCount());
            suggestion.setSuggestedKeywords(cluster.getSuggestedKeywords());
            
            String category = matchCategory(cluster, existingCategories);
            suggestion.setRelatedCategory(category);
            
            if (cluster.getTotalCount() >= 10) {
                suggestion.setPriority("高");
                suggestion.setSuggestionType("紧急补库");
                suggestion.setSuggestion("该主题问题频繁出现，建议立即补充相关知识库文档");
            } else if (cluster.getTotalCount() >= 5) {
                suggestion.setPriority("中");
                suggestion.setSuggestionType("建议补库");
                suggestion.setSuggestion("该主题存在知识缺口，建议补充相关文档");
            } else {
                suggestion.setPriority("低");
                suggestion.setSuggestionType("观察");
                suggestion.setSuggestion("该主题问题较少，建议继续观察");
            }

            suggestions.add(suggestion);
        }

        return suggestions.stream()
            .sorted((a, b) -> {
                int priorityOrder = getPriorityOrder(b.getPriority()) - getPriorityOrder(a.getPriority());
                return priorityOrder != 0 ? priorityOrder : Integer.compare(b.getQuestionCount(), a.getQuestionCount());
            })
            .collect(Collectors.toList());
    }

    private String matchCategory(UnansweredCluster cluster, List<String> categories) {
        for (String keyword : cluster.getSuggestedKeywords()) {
            for (String category : categories) {
                if (category.contains(keyword) || keyword.contains(category)) {
                    return category;
                }
            }
        }
        return "未分类";
    }

    private int getPriorityOrder(String priority) {
        return switch (priority) {
            case "高" -> 3;
            case "中" -> 2;
            case "低" -> 1;
            default -> 0;
        };
    }

    private List<QaUnansweredExport> convertToExportData(List<QaUnanswered> records) {
        return records.stream()
            .map(r -> {
                QaUnansweredExport export = new QaUnansweredExport();
                export.setQuestion(r.getQuestion());
                export.setCount(r.getCount());
                export.setFirstOccurrence(r.getCreateTime());
                export.setLastOccurrence(r.getUpdateTime());
                return export;
            })
            .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
            .collect(Collectors.toList());
    }
}