package com.demo.aiknowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.aiknowledge.entity.QaUnanswered;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QaUnansweredMapper extends BaseMapper<QaUnanswered> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM qa_unanswered")
    java.util.List<QaUnanswered> selectAll();
}
