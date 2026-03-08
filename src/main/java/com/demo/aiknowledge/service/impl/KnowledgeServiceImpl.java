package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiknowledge.entity.KnowledgeDoc;
import com.demo.aiknowledge.mapper.KnowledgeDocMapper;
import com.demo.aiknowledge.service.AiService;
import com.demo.aiknowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final AiService aiService;

    // 1. 从配置文件读取路径，默认值为 ./uploads (但建议配置文件中必须配绝对路径)
    @Value("${upload.dir:./uploads}")
    private String uploadDirConfig;

    @Override
    public KnowledgeDoc uploadDoc(MultipartFile file, Long categoryId) {
        // 1. 保存文件
        String fileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        // 确保文件名安全，避免路径遍历风险
        if (fileName == null) fileName = "unknown";
        String savedFileName = uuid + "_" + fileName;
        
        File uploadDir = new File(uploadDirConfig);
        if (!uploadDir.exists()) {
            if (!uploadDir.mkdirs()) {
                throw new RuntimeException("Failed to create upload directory");
            }
        }
        
        File dest = new File(uploadDir, savedFileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("File upload failed", e);
            throw new RuntimeException("File upload failed");
        }

        // 2. 记录到数据库
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setDocName(fileName);
        doc.setFilePath(dest.getAbsolutePath());
        doc.setCategoryId(categoryId);
        doc.setStatus("PENDING");
        doc.setCreateTime(LocalDateTime.now());
        knowledgeDocMapper.insert(doc);

        // 3. 异步调用 AI 服务解析文档 (注意：这里在 KnowledgeServiceImpl 中调用一次，
        // 而 AdminController 又调用了一次 aiService.parseDocument。
        // 为了避免重复调用，这里可以保留，AdminController 不需要再显式调用，
        // 或者将解析逻辑剥离出来。)
        // 鉴于 AdminController 的实现使用了 uploadDoc 方法，这里保留调用即可。
        aiService.parseDocument(dest.getAbsolutePath(), doc.getId());

        return doc;
    }

    @Override
    public List<KnowledgeDoc> listDocs(Long categoryId) {
        LambdaQueryWrapper<KnowledgeDoc> query = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            query.eq(KnowledgeDoc::getCategoryId, categoryId);
        }
        query.orderByDesc(KnowledgeDoc::getCreateTime);
        return knowledgeDocMapper.selectList(query);
    }

    @Override
    public void deleteDoc(Long docId) {
        knowledgeDocMapper.deleteById(docId);
        // TODO: 同时删除物理文件和向量数据库中的数据
    }
}
