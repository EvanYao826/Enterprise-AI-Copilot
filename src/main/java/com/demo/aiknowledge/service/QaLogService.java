package com.demo.aiknowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.aiknowledge.entity.QaLog;

public interface QaLogService extends IService<QaLog> {
    void log(Long userId, String question, String answer);
}
