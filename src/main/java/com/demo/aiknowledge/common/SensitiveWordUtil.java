package com.demo.aiknowledge.common;

import cn.hutool.dfa.WordTree;
import org.springframework.stereotype.Component;

// 修复：Spring Boot 3 使用 jakarta，若为 Spring Boot 2 请改回 javax 并确保有相关依赖
import jakarta.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;

@Component
public class SensitiveWordUtil {

    private final WordTree wordTree = new WordTree();

    @PostConstruct
    public void init() {
        // 模拟敏感词库，实际可从数据库或文件加载
        List<String> sensitiveWords = Arrays.asList("暴力", "色情", "赌博", "admin", "root", "system");
        wordTree.addWords(sensitiveWords);
    }

    public boolean contains(String text) {
        if (text == null) {
            return false;
        }
        return wordTree.isMatch(text);
    }

    public String filter(String text) {
        // 优化：增加空值检查，distinct 去重避免重复替换
        if (text == null) {
            return null;
        }
        return wordTree.matchAll(text).stream()
                .distinct()
                .reduce(text, (acc, word) -> acc.replace(word, "*".repeat(word.length())));
    }
}
