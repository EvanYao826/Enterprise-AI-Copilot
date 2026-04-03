package com.demo.aiknowledge.controller;

import com.demo.aiknowledge.common.Result;
import com.demo.aiknowledge.dto.ChatRequest;
import com.demo.aiknowledge.entity.Conversation;
import com.demo.aiknowledge.entity.Message;
import com.demo.aiknowledge.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversations")
    public Result<Conversation> createConversation(@RequestParam Long userId, @RequestParam(required = false) String title) {
        return Result.success(chatService.createConversation(userId, title));
    }

    @GetMapping("/conversations")
    public Result<List<Conversation>> getHistory(@RequestParam Long userId) {
        return Result.success(chatService.getHistory(userId));
    }

    @PostMapping("/messages")
    public Result<Message> sendMessage(@RequestBody ChatRequest request) {
        return Result.success(chatService.sendMessage(request.getUserId(), request.getConversationId(), request.getContent()));
    }

    @GetMapping("/messages")
    public Result<List<Message>> getMessages(@RequestParam Long conversationId) {
        return Result.success(chatService.getMessages(conversationId));
    }

    @DeleteMapping("/conversations/{id}")
    public Result<String> deleteConversation(@PathVariable Long id) {
        chatService.deleteConversation(id);
        return Result.success("Conversation deleted");
    }

    @PutMapping("/conversations/{id}")
    public Result<Conversation> updateConversation(@PathVariable Long id, @RequestBody Conversation conversation) {
        return Result.success(chatService.updateConversation(id, conversation.getTitle(), conversation.getIsPinned()));
    }

    @GetMapping("/test-auth")
    public Result<String> testAuth() {
        return Result.success("Authentication successful - you have USER role access");
    }
}
