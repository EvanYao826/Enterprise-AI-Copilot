package com.demo.aiknowledge.controller;

import com.demo.aiknowledge.common.Result;
import com.demo.aiknowledge.entity.KnowledgeDoc;
import com.demo.aiknowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/upload")
    public Result<KnowledgeDoc> upload(@RequestParam("file") MultipartFile file, @RequestParam(required = false) Long categoryId) {
        return Result.success(knowledgeService.uploadDoc(file, categoryId));
    }

    @GetMapping("/list")
    public Result<List<KnowledgeDoc>> list(@RequestParam(required = false) Long categoryId) {
        return Result.success(knowledgeService.listDocs(categoryId));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        knowledgeService.deleteDoc(id);
        return Result.success("Deleted successfully");
    }
}
