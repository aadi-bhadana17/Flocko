package com.kilgore.fooddeliveryapp.chat.service;

import com.kilgore.fooddeliveryapp.chat.model.ChatMessage;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.chat.dto.request.SendChatMessageRequest;
import com.kilgore.fooddeliveryapp.chat.dto.response.ChatMessageResponse;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.common.exceptions.ChatNotAllowedException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import com.kilgore.fooddeliveryapp.ordering.model.Order;
import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import com.kilgore.fooddeliveryapp.chat.repository.ChatRepository;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
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
    private final UserFacade userFacade;
    private final UserRepository userRepository;

    public ChatService(ChatRepository chatRepository, UserAuthorization userAuthorization, OrderRepository orderRepository, UserFacade userFacade, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.userAuthorization = userAuthorization;
        this.orderRepository = orderRepository;
        this.userFacade = userFacade;
        this.userRepository = userRepository;
    }


    public List<ChatMessageResponse> getMessages(Long orderId) {
        Long userId = userAuthorization.authorizeUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (userFacade.isUserCustomer(userId) && !Objects.equals(order.getUserId(), userId))
            throw new AccessDeniedException("Customers can only view messages for their own orders");

        else if (userFacade.isUserRestaurantOwner(userId) && !userFacade.isOwnerOfRestaurant(userId, order.getRestaurantId()))
            throw new AccessDeniedException("Restaurant owner can only view messages for their own restaurant's orders");

        else if (userFacade.isUserRestaurantStaff(userId) && !userFacade.isEmployedAt(userId, order.getRestaurantId()))
            throw new AccessDeniedException("Restaurant staff can only view messages for their own restaurant's orders");

        return chatRepository.getMessages(orderId).stream()
                .map(chatMessage -> createChatMessageResponse(chatMessage, chatMessage.getSender()))
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long orderId, SendChatMessageRequest request) {
        Long userId = userAuthorization.authorizeUserId();
        User user = getUser(userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if(!order.isSpecial())
            throw new ChatNotAllowedException("Only special orders can have chat messages");

        if(order.getOrderStatus() == OrderStatus.CANCELLED)
            throw new ChatNotAllowedException("Cannot send messages for cancelled orders");
        else if(order.getOrderStatus() == OrderStatus.DELIVERED)
            throw new ChatNotAllowedException("Cannot send messages for delivered orders");

        if(userFacade.isUserCustomer(userId) && !userId.equals(order.getUserId()))
            throw new AccessDeniedException("Only the customer who placed the order can send messages");
        else if(userFacade.isUserRestaurantStaff(userId) && !userFacade.isEmployedAt(userId, order.getRestaurantId()))
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

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
