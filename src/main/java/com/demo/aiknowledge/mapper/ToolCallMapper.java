package com.demo.aiknowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.aiknowledge.entity.ToolCall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ToolCallMapper extends BaseMapper<ToolCall> {
    
    @Select("SELECT * FROM tool_call WHERE run_id = #{runId} ORDER BY timestamp ASC")
    List<ToolCall> selectByRunId(String runId);
    
    @Select("SELECT * FROM tool_call WHERE status = 'failed' ORDER BY timestamp DESC LIMIT #{limit}")
    List<ToolCall> selectFailedToolCalls(int limit);
}
