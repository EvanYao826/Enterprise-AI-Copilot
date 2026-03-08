package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.aiknowledge.entity.QaLog;
import com.demo.aiknowledge.mapper.QaLogMapper;
import com.demo.aiknowledge.service.QaLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QaLogServiceImpl extends ServiceImpl<QaLogMapper, QaLog> implements QaLogService {

    @Override
    @Async // 异步记录日志，不影响主业务性能
    public void log(Long userId, String question, String answer) {
        QaLog log = new QaLog();
        log.setUserId(userId);
        log.setQuestion(question);
        log.setAnswer(answer);
        log.setCreateTime(LocalDateTime.now());
        this.save(log);
    }
}
