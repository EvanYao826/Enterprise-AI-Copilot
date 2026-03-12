package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.aiknowledge.entity.QaUnanswered;
import com.demo.aiknowledge.mapper.QaUnansweredMapper;
import com.demo.aiknowledge.service.QaUnansweredService;
import org.springframework.stereotype.Service;

@Service
public class QaUnansweredServiceImpl extends ServiceImpl<QaUnansweredMapper, QaUnanswered> implements QaUnansweredService {

    @Override
    public void recordUnansweredQuestion(String question) {
        QaUnanswered existing = this.getOne(new LambdaQueryWrapper<QaUnanswered>()
                .eq(QaUnanswered::getQuestion, question));
        
        if (existing != null) {
            existing.setCount(existing.getCount() + 1);
            this.updateById(existing);
        } else {
            QaUnanswered newRecord = new QaUnanswered();
            newRecord.setQuestion(question);
            newRecord.setCount(1);
            this.save(newRecord);
        }
    }
}
