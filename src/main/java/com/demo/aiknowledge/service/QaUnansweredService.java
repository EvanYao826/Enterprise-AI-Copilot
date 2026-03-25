package com.demo.aiknowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.aiknowledge.entity.QaUnanswered;

public interface QaUnansweredService extends IService<QaUnanswered> {
    void recordUnansweredQuestion(String question);
}
