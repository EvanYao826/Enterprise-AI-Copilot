package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.aiknowledge.entity.Notice;
import com.demo.aiknowledge.mapper.NoticeMapper;
import com.demo.aiknowledge.service.NoticeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public List<Notice> getActiveNotices() {
        return this.list(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getIsActive, true)
                .orderByDesc(Notice::getCreateTime));
    }
}
