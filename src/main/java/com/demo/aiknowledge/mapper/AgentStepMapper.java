package com.demo.aiknowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.aiknowledge.entity.AgentStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentStepMapper extends BaseMapper<AgentStep> {
    
    @Select("SELECT * FROM agent_step WHERE run_id = #{runId} ORDER BY created_at ASC")
    List<AgentStep> selectByRunId(String runId);
    
    @Select("SELECT * FROM agent_step WHERE status = #{status} ORDER BY created_at DESC")
    List<AgentStep> selectByStatus(String status);
}
