package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.authorization.UserAuthorization;
import com.kilgore.fooddeliveryapp.dto.request.SendChatMessageRequest;
import com.kilgore.fooddeliveryapp.dto.response.ChatMessageResponse;
import com.kilgore.fooddeliveryapp.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.exceptions.ChatNotAllowedException;
import com.kilgore.fooddeliveryapp.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.model.*;
import com.kilgore.fooddeliveryapp.repository.ChatRepository;
import com.kilgore.fooddeliveryapp.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserAuthorization userAuthorization;
    private final OrderRepository orderRepository;

    public ChatService(ChatRepository chatRepository, UserAuthorization userAuthorization, OrderRepository orderRepository) {
        this.chatRepository = chatRepository;
        this.userAuthorization = userAuthorization;
        this.orderRepository = orderRepository;
    }


    public List<ChatMessageResponse> getMessages(Long orderId) {
        User user = userAuthorization.authorizeUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if(user.getRole() == UserRole.CUSTOMER && !Objects.equals(order.getUser().getUserId(), user.getUserId()))
            throw new AccessDeniedException("Customers can only view messages for their own orders");

        else if(user.getRole() == UserRole.RESTAURANT_OWNER && !Objects.equals(order.getRestaurant().getOwner().getUserId(), user.getUserId()))
            throw new AccessDeniedException("Restaurant owner can only view messages for their own restaurant's orders");

        else if(user.getRole() == UserRole.RESTAURANT_STAFF && !Objects.equals(order.getRestaurant().getRestaurantId(), user.getEmployedAt()))
            throw new AccessDeniedException("Restaurant staff can only view messages for their own restaurant's orders");

        return chatRepository.getMessages(orderId).stream()
                .map(chatMessage -> createChatMessageResponse(chatMessage, chatMessage.getSender()))
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long orderId, SendChatMessageRequest request) {
        User user = userAuthorization.authorizeUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if(!order.isSpecial())
            throw new ChatNotAllowedException("Only special orders can have chat messages");

        if(order.getOrderStatus() == OrderStatus.CANCELLED)
            throw new ChatNotAllowedException("Cannot send messages for cancelled orders");
        else if(order.getOrderStatus() == OrderStatus.DELIVERED)
            throw new ChatNotAllowedException("Cannot send messages for delivered orders");

        if(user.getRole() == UserRole.CUSTOMER && !user.getUserId().equals(order.getUser().getUserId()))
            throw new AccessDeniedException("Only the customer who placed the order can send messages");
        else if(user.getRole() == UserRole.RESTAURANT_STAFF && !Objects.equals(order.getRestaurant().getRestaurantId(), user.getEmployedAt()))
            throw new AccessDeniedException("Restaurant staff can only send messages for their own restaurant's orders");

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setOrder(order);
        chatMessage.setSender(user);
        chatMessage.setMessage(request.getMessage());
        chatMessage.setTimestamp(LocalDateTime.now());

        chatRepository.save(chatMessage);

        return createChatMessageResponse(chatMessage, chatMessage.getSender());
    }

    private ChatMessageResponse createChatMessageResponse(ChatMessage chatMessage, User sender) {
        UserSummary senderSummary = new UserSummary(
                sender.getUserId(),
                sender.getFirstName() + " " + sender.getLastName()
        );

        return new ChatMessageResponse(
                chatMessage.getMessageId(),
                chatMessage.getMessage(),
                senderSummary,
                chatMessage.getTimestamp()
        );
    }
}
