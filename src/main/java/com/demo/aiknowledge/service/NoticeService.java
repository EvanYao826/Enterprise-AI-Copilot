package com.demo.aiknowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.aiknowledge.entity.Notice;
import java.util.List;

public interface NoticeService extends IService<Notice> {
    List<Notice> getActiveNotices();
}
