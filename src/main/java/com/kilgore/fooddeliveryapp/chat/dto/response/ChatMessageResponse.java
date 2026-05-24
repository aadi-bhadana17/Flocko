package com.kilgore.fooddeliveryapp.chat.dto.response;

import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    private Long messageId;
    private String message;
    private UserSummary sender;
    private LocalDateTime timestamp;
}
