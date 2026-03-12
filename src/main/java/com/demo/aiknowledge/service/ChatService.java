package com.demo.aiknowledge.service;

import com.demo.aiknowledge.entity.Conversation;
import com.demo.aiknowledge.entity.Message;
import java.util.List;

public interface ChatService {
    Conversation createConversation(Long userId, String title);
    List<Conversation> getHistory(Long userId);
    Message sendMessage(Long userId, Long conversationId, String content);
    List<Message> getMessages(Long conversationId);
    void deleteConversation(Long conversationId);
    Conversation updateConversation(Long conversationId, String title, Boolean isPinned);
}
