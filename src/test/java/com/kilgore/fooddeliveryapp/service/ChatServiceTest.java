package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.chat.dto.request.SendChatMessageRequest;
import com.kilgore.fooddeliveryapp.chat.dto.response.ChatMessageResponse;
import com.kilgore.fooddeliveryapp.chat.model.ChatMessage;
import com.kilgore.fooddeliveryapp.chat.repository.ChatRepository;
import com.kilgore.fooddeliveryapp.chat.service.ChatService;
import com.kilgore.fooddeliveryapp.common.exceptions.ChatNotAllowedException;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.util.UserAuthorization;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import com.kilgore.fooddeliveryapp.ordering.model.Order;
import com.kilgore.fooddeliveryapp.ordering.model.OrderStatus;
import com.kilgore.fooddeliveryapp.ordering.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long OTHER_CUSTOMER_ID = 2L;
    private static final Long STAFF_ID = 1L;
    private static final Long OWNER_ID = 1L;
    private static final Long RESTAURANT_ID = 10L;
    private static final Long ORDER_ID = 100L;

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAuthorization userAuthorization;
    @Mock
    private UserFacade userFacade;

    @InjectMocks
    private ChatService chatService;

    @Test
    void getMessages_returnsMappedMessagesForCustomer() {
        User customer = createUser(CUSTOMER_ID, "customer@example.com", UserRole.CUSTOMER);
        User staff = createUser(2L, "staff@example.com", UserRole.RESTAURANT_STAFF);
        Order order = createOrder(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, OrderStatus.PREPARING, true);
        ChatMessage first = createMessage(11L, order, staff, "On the way");
        ChatMessage second = createMessage(12L, order, customer, "Thanks");

        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(userFacade.isUserCustomer(CUSTOMER_ID)).thenReturn(true);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(chatRepository.getMessages(ORDER_ID)).thenReturn(List.of(first, second));

        List<ChatMessageResponse> responses = chatService.getMessages(ORDER_ID);

        assertEquals(2, responses.size());
        assertEquals("On the way", responses.get(0).getMessage());
        assertEquals("Test User", responses.get(0).getSender().getUserName());
    }

    @Test
    void getMessages_throwsWhenOrderNotFound() {
        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> chatService.getMessages(99L));
        verify(chatRepository, never()).getMessages(anyLong());
    }

    @Test
    void getMessages_throwsWhenCustomerRequestsOtherOrder() {
        Order order = createOrder(ORDER_ID, OTHER_CUSTOMER_ID, RESTAURANT_ID, OrderStatus.PREPARING, true);

        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(userFacade.isUserCustomer(CUSTOMER_ID)).thenReturn(true);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> chatService.getMessages(ORDER_ID));
        verify(chatRepository, never()).getMessages(anyLong());
    }

    @Test
    void getMessages_throwsWhenOwnerRequestsOtherRestaurantOrder() {
        Order order = createOrder(ORDER_ID, OTHER_CUSTOMER_ID, RESTAURANT_ID, OrderStatus.PREPARING, true);

        when(userAuthorization.authorizeUserId()).thenReturn(OWNER_ID);
        when(userFacade.isUserCustomer(OWNER_ID)).thenReturn(false);
        when(userFacade.isUserRestaurantOwner(OWNER_ID)).thenReturn(true);
        when(userFacade.isOwnerOfRestaurant(OWNER_ID, RESTAURANT_ID)).thenReturn(false);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> chatService.getMessages(ORDER_ID));
        verify(chatRepository, never()).getMessages(anyLong());
    }

    @Test
    void getMessages_throwsWhenStaffRequestsOtherRestaurantOrder() {
        Order order = createOrder(ORDER_ID, OTHER_CUSTOMER_ID, RESTAURANT_ID, OrderStatus.PREPARING, true);

        when(userAuthorization.authorizeUserId()).thenReturn(STAFF_ID);
        when(userFacade.isUserCustomer(STAFF_ID)).thenReturn(false);
        when(userFacade.isUserRestaurantOwner(STAFF_ID)).thenReturn(false);
        when(userFacade.isUserRestaurantStaff(STAFF_ID)).thenReturn(true);
        when(userFacade.isEmployedAt(STAFF_ID, RESTAURANT_ID)).thenReturn(false);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> chatService.getMessages(ORDER_ID));
        verify(chatRepository, never()).getMessages(anyLong());
    }

    @Test
    void getMessages_throwsWhenUnauthenticated() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class, () -> chatService.getMessages(ORDER_ID));
        verify(orderRepository, never()).findById(anyLong());
    }

    @Test
    void sendMessage_createsMessageForCustomer() {
        User customer = createUser(CUSTOMER_ID, "customer@example.com", UserRole.CUSTOMER);
        Order order = createOrder(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, OrderStatus.CONFIRMED, true);
        SendChatMessageRequest request = new SendChatMessageRequest("Hello");

        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(userFacade.isUserCustomer(CUSTOMER_ID)).thenReturn(true);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setMessageId(55L);
            return saved;
        });

        ChatMessageResponse response = chatService.sendMessage(ORDER_ID, request);

        assertEquals("Hello", response.getMessage());
        assertEquals("Test User", response.getSender().getUserName());
        assertNotNull(response.getTimestamp());

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatRepository).save(captor.capture());
        assertEquals(order, captor.getValue().getOrder());
        assertEquals(customer, captor.getValue().getSender());
    }

    @Test
    void sendMessage_throwsWhenOrderNotFound() {
        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(createUser(CUSTOMER_ID, "customer@example.com", UserRole.CUSTOMER)));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> chatService.sendMessage(99L, new SendChatMessageRequest("Hi")));
        verify(chatRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_throwsWhenOrderNotSpecial() {
        Order order = createOrder(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, OrderStatus.CONFIRMED, false);

        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(createUser(CUSTOMER_ID, "customer@example.com", UserRole.CUSTOMER)));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(ChatNotAllowedException.class, () -> chatService.sendMessage(ORDER_ID, new SendChatMessageRequest("Hi")));
        verify(chatRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_throwsWhenOrderCancelled() {
        Order order = createOrder(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, OrderStatus.CANCELLED, true);

        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(createUser(CUSTOMER_ID, "customer@example.com", UserRole.CUSTOMER)));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(ChatNotAllowedException.class, () -> chatService.sendMessage(ORDER_ID, new SendChatMessageRequest("Hi")));
        verify(chatRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_throwsWhenOrderDelivered() {
        Order order = createOrder(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, OrderStatus.DELIVERED, true);

        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(createUser(CUSTOMER_ID, "customer@example.com", UserRole.CUSTOMER)));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(ChatNotAllowedException.class, () -> chatService.sendMessage(ORDER_ID, new SendChatMessageRequest("Hi")));
        verify(chatRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_throwsWhenCustomerNotOrderOwner() {
        Order order = createOrder(ORDER_ID, OTHER_CUSTOMER_ID, RESTAURANT_ID, OrderStatus.CONFIRMED, true);

        when(userAuthorization.authorizeUserId()).thenReturn(CUSTOMER_ID);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(createUser(CUSTOMER_ID, "customer@example.com", UserRole.CUSTOMER)));
        when(userFacade.isUserCustomer(CUSTOMER_ID)).thenReturn(true);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> chatService.sendMessage(ORDER_ID, new SendChatMessageRequest("Hi")));
        verify(chatRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_throwsWhenStaffFromOtherRestaurant() {
        Order order = createOrder(ORDER_ID, OTHER_CUSTOMER_ID, RESTAURANT_ID, OrderStatus.CONFIRMED, true);

        when(userAuthorization.authorizeUserId()).thenReturn(STAFF_ID);
        when(userRepository.findById(STAFF_ID)).thenReturn(Optional.of(createUser(STAFF_ID, "staff@example.com", UserRole.RESTAURANT_STAFF)));
        when(userFacade.isUserCustomer(STAFF_ID)).thenReturn(false);
        when(userFacade.isUserRestaurantStaff(STAFF_ID)).thenReturn(true);
        when(userFacade.isEmployedAt(STAFF_ID, RESTAURANT_ID)).thenReturn(false);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> chatService.sendMessage(ORDER_ID, new SendChatMessageRequest("Hi")));
        verify(chatRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_throwsWhenUnauthenticated() {
        when(userAuthorization.authorizeUserId()).thenThrow(new AccessDeniedException("No valid authority"));

        assertThrows(AccessDeniedException.class, () -> chatService.sendMessage(ORDER_ID, new SendChatMessageRequest("Hi")));
        verify(orderRepository, never()).findById(anyLong());
    }

    private User createUser(Long id, String email, UserRole role) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Order createOrder(Long id, Long userId, Long restaurantId, OrderStatus status, boolean special) {
        Order order = new Order();
        order.setOrderId(id);
        order.setUserId(userId);
        order.setRestaurantId(restaurantId);
        order.setOrderStatus(status);
        order.setSpecial(special);
        return order;
    }

    private ChatMessage createMessage(Long id, Order order, User sender, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessageId(id);
        chatMessage.setOrder(order);
        chatMessage.setSender(sender);
        chatMessage.setMessage(message);
        chatMessage.setTimestamp(LocalDateTime.now());
        return chatMessage;
    }
}
