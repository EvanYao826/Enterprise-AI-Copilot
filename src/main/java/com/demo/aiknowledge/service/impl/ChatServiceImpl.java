package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiknowledge.entity.Conversation;
import com.demo.aiknowledge.entity.Message;
import com.demo.aiknowledge.entity.QaLog;
import com.demo.aiknowledge.mapper.ConversationMapper;
import com.demo.aiknowledge.mapper.MessageMapper;
import com.demo.aiknowledge.mapper.QaLogMapper;
import com.demo.aiknowledge.service.AiService;
import com.demo.aiknowledge.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final QaLogMapper qaLogMapper;
    private final AiService aiService;

    @Override
    public Conversation createConversation(Long userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title != null ? title : "New Chat " + LocalDateTime.now());
        conversation.setCreateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public List<Conversation> getHistory(Long userId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getCreateTime));
    }

    @Override
    @Transactional
    public Message sendMessage(Long userId, Long conversationId, String content) {
        // 1. 保存用户消息
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 检查是否为第一条消息，如果是则生成标题
        Long msgCount = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId));
        if (msgCount <= 1) { // 只有刚刚插入的这一条
             // 异步生成标题，避免阻塞
             // 注意：Spring 的 @Async 需要在另一个类调用才生效，或者通过 self-injection
             // 这里 AiService.generateTitle 已经是 @Async 了，所以直接调用即可
             aiService.generateTitle(conversationId, content);
        }

        // 2. 调用 AI 服务获取回答
        // 这里可以获取上下文，为了简化，暂时只传当前问题
        String aiResponse = aiService.ask(content, ""); 

        // 3. 保存 AI 回答
        Message aiMsg = new Message();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiResponse);
        aiMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(aiMsg);

        // 4. 记录 QA 日志
        QaLog qaLog = new QaLog();
        qaLog.setUserId(userId);
        qaLog.setQuestion(content);
        qaLog.setAnswer(aiResponse);
        qaLog.setCreateTime(LocalDateTime.now());
        qaLogMapper.insert(qaLog);

        return aiMsg; // 返回 AI 的回答
    }

    @Override
    public List<Message> getMessages(Long conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getCreateTime));
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId) {
        // 删除会话相关的消息
        messageMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getConversationId, conversationId));
        // 删除会话本身
        conversationMapper.deleteById(conversationId);
    }
}
