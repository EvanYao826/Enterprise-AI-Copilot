package com.demo.aiknowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.aiknowledge.entity.AgentRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentRunMapper extends BaseMapper<AgentRun> {
    
    @Select("SELECT * FROM agent_run WHERE run_id = #{runId}")
    AgentRun selectByRunId(String runId);
    
    @Select("SELECT * FROM agent_run WHERE conversation_id = #{conversationId} ORDER BY start_time DESC")
    List<AgentRun> selectByConversationId(String conversationId);
    
    @Select("SELECT * FROM agent_run WHERE user_id = #{userId} ORDER BY start_time DESC")
    List<AgentRun> selectByUserId(String userId);
    
    @Select("SELECT * FROM agent_run WHERE status = #{status} ORDER BY start_time DESC")
    List<AgentRun> selectByStatus(String status);
}
