package com.kilgore.fooddeliveryapp.dto.response;

import com.kilgore.fooddeliveryapp.model.RequestStatus;
import com.kilgore.fooddeliveryapp.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleChangeRequestResponse {

    private Long requestId;
    private String userName;
    private String userEmail;
    private UserRole requestedRole;
    private RequestStatus requestStatus;

    private String requestReason;

    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;

    private String adminName;
    private String adminEmail;

}
