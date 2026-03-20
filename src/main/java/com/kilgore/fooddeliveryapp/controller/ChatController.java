package com.kilgore.fooddeliveryapp.controller;

import com.kilgore.fooddeliveryapp.dto.request.SendChatMessageRequest;
import com.kilgore.fooddeliveryapp.dto.response.ChatMessageResponse;
import com.kilgore.fooddeliveryapp.service.ChatService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order/{orderId}/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<ChatMessageResponse> getMessages(@PathVariable Long orderId) {
        return chatService.getMessages(orderId);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'RESTAURANT_STAFF')")
    public ChatMessageResponse sendMessage(@PathVariable Long orderId,
                                           @RequestBody SendChatMessageRequest request) {
        return chatService.sendMessage(orderId, request);
    }
}
