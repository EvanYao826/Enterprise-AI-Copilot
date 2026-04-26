package com.demo.aiknowledge.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.aiknowledge.common.Result;
import com.demo.aiknowledge.entity.Admin;
import com.demo.aiknowledge.entity.KnowledgeDoc;
import com.demo.aiknowledge.entity.User;
import com.demo.aiknowledge.service.AdminService;
import com.demo.aiknowledge.service.AiService;
import com.demo.aiknowledge.service.KnowledgeService;
import com.demo.aiknowledge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import com.demo.aiknowledge.entity.QaLog;
import com.demo.aiknowledge.service.QaLogService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final KnowledgeService knowledgeService;
    private final AiService aiService;
    private final QaLogService qaLogService;

    // --- 登录 ---
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String username, @RequestParam String password) {
        return Result.success(adminService.login(username, password));
    }

    // --- 用户管理 ---
    @GetMapping("/users")
    public Result<IPage<User>> listUsers(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(userService.page(new Page<>(page, size)));
    }

    @PostMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        userService.updateStatus(userId, status);
        return Result.success(null);
    }

    // --- 知识库管理 ---
    @PostMapping("/knowledge/upload")
    public Result<KnowledgeDoc> uploadDoc(@RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) Long categoryId) {
        // KnowledgeService.saveDoc 内部已经包含了调用 AI 解析的逻辑
        KnowledgeDoc doc = knowledgeService.saveDoc(file, categoryId);
        
        // 因此不需要再次显式调用 aiService.parseDocument，避免重复解析
        
        return Result.success(doc);
    }

    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteDoc(@PathVariable Long id) {
        // 需要同时删除向量库中的数据 (这里暂时只删除了数据库记录和文件)
        // 完善建议：Python端增加删除向量接口
        knowledgeService.deleteDoc(id);
        return Result.success(null);
    }

    @GetMapping("/knowledge/list")
    public Result<List<KnowledgeDoc>> listDocs(@RequestParam(required = false) Long categoryId) {
        return Result.success(knowledgeService.listDocs(categoryId));
    }

    @PostMapping("/knowledge/retry-parse")
    public Result<Void> retryParse(@RequestBody Map<String, Object> request) {
        Long id = Long.parseLong(request.get("id").toString());
        String filePath = (String) request.get("filePath");
        aiService.parseDocument(filePath, id);
        return Result.success(null);
    }
    
    // --- 问答日志管理 ---
    @GetMapping("/logs")
    public Result<IPage<QaLog>> listLogs(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size) {
        // 按时间倒序查询，最新的在前面
        Page<QaLog> logPage = new Page<>(page, size);
        return Result.success(qaLogService.page(logPage, 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QaLog>()
                .orderByDesc(QaLog::getCreateTime)));
    }
}
