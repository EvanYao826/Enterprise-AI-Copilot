package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiknowledge.entity.DocViewLog;
import com.demo.aiknowledge.entity.KnowledgeDoc;
import com.demo.aiknowledge.mapper.DocViewLogMapper;
import com.demo.aiknowledge.mapper.KnowledgeDocMapper;
import com.demo.aiknowledge.service.AiService;
import com.demo.aiknowledge.service.KnowledgeService;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.BucketManager;
import com.qiniu.util.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final DocViewLogMapper docViewLogMapper;
    private final AiService aiService;

    // Qiniu Cloud Kodo 配置
    @Value("${qiniu.accessKey:}")
    private String qiniuAccessKey;
    @Value("${qiniu.secretKey:}")
    private String qiniuSecretKey;
    @Value("${qiniu.bucket:}")
    private String qiniuBucket;
    @Value("${qiniu.domain:}")
    private String qiniuDomain;

    @Override
    public KnowledgeDoc uploadDoc(MultipartFile file, Long categoryId) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) fileName = "unknown";
        String uuid = UUID.randomUUID().toString();
        String savedFileName = uuid + "_" + fileName;
        String filePath;

        // 判断是否配置了 Qiniu，如果配置了则上传到 Qiniu，否则保存到本地
        if (qiniuAccessKey != null && !qiniuAccessKey.isEmpty()) {
            try {
                uploadToQiniu("documents/" + savedFileName, file.getInputStream());
                // Qiniu 文件路径 (URL)
                // 确保 domain 结尾没有 /
                String domain = qiniuDomain.endsWith("/") ? qiniuDomain.substring(0, qiniuDomain.length() - 1) : qiniuDomain;
                // 强制添加协议头，如果未配置
                if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
                    domain = "http://" + domain;
                }
                filePath = domain + "/documents/" + savedFileName;
            } catch (IOException e) {
                log.error("Qiniu upload failed", e);
                throw new RuntimeException("Qiniu upload failed");
            }
        } else {
            // 保存到本地
            try {
                String uploadDir = "D:/aiknowledge/uploads";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File savedFile = new File(dir, savedFileName);
                file.transferTo(savedFile);
                filePath = savedFile.getAbsolutePath();
            } catch (IOException e) {
                log.error("Local file save failed", e);
                throw new RuntimeException("Local file save failed");
            }
        }

        // 2. 记录到数据库
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setDocName(fileName);
        doc.setFilePath(filePath);
        doc.setCategoryId(categoryId);
        doc.setStatus("PENDING");
        doc.setCreateTime(LocalDateTime.now());
        knowledgeDocMapper.insert(doc);

        // 3. 异步调用 AI 服务解析文档
        aiService.parseDocument(filePath, doc.getId());

        return doc;
    }

    private void uploadToQiniu(String objectName, InputStream inputStream) {
        // 构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.autoRegion());
        UploadManager uploadManager = new UploadManager(cfg);
        Auth auth = Auth.create(qiniuAccessKey, qiniuSecretKey);
        String upToken = auth.uploadToken(qiniuBucket);

        try {
            Response response = uploadManager.put(inputStream, objectName, upToken, null, null);
            // 解析上传成功的结果
            // DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            log.info("Qiniu upload success: {}", response.bodyString());
        } catch (QiniuException ex) {
            log.error("Qiniu upload failed", ex);
            if (ex.response != null) {
                log.error("Qiniu error response: {}", ex.response.toString());
            }
            throw new RuntimeException("Qiniu upload failed");
        }
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
        KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc != null) {
            log.info("Deleting document: id={}, name={}", docId, doc.getDocName());
            
            // 删除 Qiniu 文件
            if (qiniuAccessKey != null && !qiniuAccessKey.isEmpty() && doc.getFilePath().startsWith("http")) {
                 try {
                     // 从 URL 中提取 key
                     // URL: http://domain/key
                     String domain = qiniuDomain.endsWith("/") ? qiniuDomain.substring(0, qiniuDomain.length() - 1) : qiniuDomain;
                     // 确保 domain 和 filePath 中的协议头一致性处理
                     // 这里为了稳健，直接找到第三个 / 之后的部分作为 Key (http://domain/key)
                     String filePath = doc.getFilePath();
                     String key = filePath;
                     
                     // 方法1：移除 domain 前缀
                     // 需要处理 domain 可能没带协议头的情况
                     String domainNoProtocol = domain.replace("http://", "").replace("https://", "");
                     if (filePath.contains(domainNoProtocol)) {
                         int index = filePath.indexOf(domainNoProtocol);
                         // + domainNoProtocol.length() + 1 (for /)
                         if (index + domainNoProtocol.length() + 1 < filePath.length()) {
                             key = filePath.substring(index + domainNoProtocol.length() + 1);
                         }
                     }
                     
                     deleteFromQiniu(key);
                     log.info("Deleted file from Qiniu: key={}", key);
                 } catch (Exception e) {
                     log.error("Delete from Qiniu failed", e);
                 }
            }
            
            // 调用 AI 服务删除向量索引
            try {
                aiService.deleteDoc(docId);
                log.info("Deleted vector index for document: id={}", docId);
            } catch (Exception e) {
                log.error("Delete vector index failed", e);
            }
            
            knowledgeDocMapper.deleteById(docId);
            log.info("Document deleted successfully: id={}, name={}", docId, doc.getDocName());
        } else {
            log.warn("Attempt to delete non-existent document: id={}", docId);
        }
    }

    private void deleteFromQiniu(String key) {
        Configuration cfg = new Configuration(Region.autoRegion());
        Auth auth = Auth.create(qiniuAccessKey, qiniuSecretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            bucketManager.delete(qiniuBucket, key);
        } catch (QiniuException ex) {
            log.error("Qiniu delete failed", ex);
            // 如果是 612 (文件不存在)，可以忽略
            if (ex.code() != 612) {
                throw new RuntimeException("Qiniu delete failed");
            }
        }
    }

    @Override
    public KnowledgeDoc viewDoc(Long docId, Long userId) {
        KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc != null) {
            DocViewLog log = new DocViewLog();
            log.setDocId(docId);
            log.setUserId(userId);
            log.setCreateTime(LocalDateTime.now());
            docViewLogMapper.insert(log);
        }
        return doc;
    }
}
