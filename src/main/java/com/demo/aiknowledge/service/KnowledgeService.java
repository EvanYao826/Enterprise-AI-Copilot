package com.demo.aiknowledge.service;

import com.demo.aiknowledge.entity.KnowledgeDoc;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface KnowledgeService {
    KnowledgeDoc uploadDoc(MultipartFile file, Long categoryId);
    
    // Admin uses this alias for now, or we can unify method names
    default KnowledgeDoc saveDoc(MultipartFile file, Long categoryId) {
        return uploadDoc(file, categoryId);
    }
    
    List<KnowledgeDoc> listDocs(Long categoryId);
    void deleteDoc(Long docId);
}
