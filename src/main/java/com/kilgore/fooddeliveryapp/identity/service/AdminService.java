package com.kilgore.fooddeliveryapp.identity.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.identity.model.RequestStatus;
import com.kilgore.fooddeliveryapp.identity.dto.request.RoleChangeRequestDecisionDto;
import com.kilgore.fooddeliveryapp.identity.dto.request.UserRestrictionDto;
import com.kilgore.fooddeliveryapp.identity.dto.response.RoleChangeRequestResponse;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserExtendedSummary;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.InvalidResponseForRoleChangeRequest;
import com.kilgore.fooddeliveryapp.common.exceptions.RequestNotFoundException;
import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.RoleChangeRequest;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.repository.RoleChangeRequestRepository;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import com.kilgore.fooddeliveryapp.identity.util.UserMapper;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {


    private final RoleChangeRequestRepository roleChangeRequestRepository;
    private final UserRepository userRepository;
    private final CatalogFacade catalogFacade;
    private final UserMapper userMapper;

    public AdminService(RoleChangeRequestRepository roleChangeRequestRepository, UserRepository userRepository, CatalogFacade catalogFacade, UserMapper userMapper) {
        this.roleChangeRequestRepository = roleChangeRequestRepository;
        this.userRepository = userRepository;
        this.catalogFacade = catalogFacade;
        this.userMapper = userMapper;
    }

    // ----------------------------------------------VIEW ALL USERS--------------------------------------------------

    public List<UserExtendedSummary> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserExtendedSummary)
                .toList();
    }

    public UserExtendedSummary getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));

        return userMapper.toUserExtendedSummary(user);
    }

    // ----------------------------------------------MANAGE USER ACCOUNTS----------------------------------------------

    public String restrictUser(Long userId, UserRestrictionDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));

        user.setAccountStatus(AccountStatus.RESTRICTED);
        user.setRestrictedUntil(LocalDateTime.now().plusDays(request.getDurationInDays()));
        user.setRestrictionReason(request.getReason());
        userRepository.save(user);

        boolean isRestaurantClosed = false;

        if(user.getRole() == UserRole.RESTAURANT_OWNER) {
            catalogFacade.getOwnedRestaurantIds(userId)
                    .forEach(catalogFacade::suspendRestaurant);
            isRestaurantClosed = true;
        }

        String response = "User " + user.getFirstName() + " " + user.getLastName()
                + " has been restricted until " + user.getRestrictedUntil()
                + " for reason: " + request.getReason();


        return isRestaurantClosed ? response + ". And all of their restaurants have been closed temporary." : response;
    }

    public String unrestrictUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRestrictedUntil(null);
        user.setRestrictionReason(null);
        userRepository.save(user);

        return "User " + user.getFirstName() + " " + user.getLastName()
                + " has been unrestricted and can placed orders from the Flocko again.";
    }

    public String blockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));

        user.setAccountStatus(AccountStatus.BLOCKED);

        boolean isRestaurantSuspended = false;

        if(user.getRole() == UserRole.RESTAURANT_OWNER) {
            catalogFacade.getOwnedRestaurantIds(userId)
                    .forEach(catalogFacade::suspendRestaurant);
            isRestaurantSuspended = true;
        }

        userRepository.save(user);

        String response = "User " + user.getFirstName() + " " + user.getLastName()
                + " has been blocked and cannot access the Flocko anymore.";

        return  isRestaurantSuspended ? response + " And all of their restaurants have been suspended too." : response;
    }

    public String unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));

        user.setAccountStatus(AccountStatus.ACTIVE);

        boolean isRestaurantReopened = false;

        if(user.getRole() == UserRole.RESTAURANT_OWNER) {
            catalogFacade.getOwnedRestaurantIds(userId)
                    .forEach(catalogFacade::reactivateRestaurant);
            isRestaurantReopened = true;
        }

        userRepository.save(user);

        String response = "User " + user.getFirstName() + " " + user.getLastName()
                + " has been unblocked and can access the Flocko again.";

        return isRestaurantReopened ? response + " And all of their restaurants have been reopened too." : response;
    }

    public String deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));

        user.setAccountStatus(AccountStatus.DELETED);
        userRepository.save(user);

        return "User " + user.getFirstName() + " " + user.getLastName()
                + " has been deleted from Flocko.";
    }

    // --------------------------------------------- MANAGE RESTAURANTS ------------------------------------------------

    @Transactional
    public String suspendRestaurant(Long restaurantId) {
        return catalogFacade.suspendRestaurant(restaurantId);
    }

    @Transactional
    public String activateRestaurant(Long restaurantId) {
        return catalogFacade.reactivateRestaurant(restaurantId);
    }

    // ----------------------------------------------MANAGE ROLE CHANGE REQUESTS----------------------------------------

    public List<RoleChangeRequestResponse> getAllRoleRequests() {
        return roleChangeRequestRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public RoleChangeRequestResponse updateRole(RoleChangeRequestDecisionDto request, Long requestId) {
        RoleChangeRequest roleChangeRequest = roleChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User admin = userRepository.findByEmail(authentication.getName());

        if(request.getAction().equalsIgnoreCase("approve")) {
            User user = roleChangeRequest.getUser();

            roleChangeRequest.setRespondedAt(LocalDateTime.now());
            roleChangeRequest.setHandledBy(admin);
            roleChangeRequest.setRequestStatus(RequestStatus.APPROVED);
            roleChangeRequestRepository.save(roleChangeRequest);

            user.setRole(roleChangeRequest.getRequestedRole()); // role has been changed and user is saved in db
            // but u know what, if a user is currently logged in as CUSTOMER and during that time he got the authority of OWNER
            // by ADMIN, so the current session of log-in will treat him as CUSTOMER, and OWNER from next

            userRepository.save(user);
        }
        else if(request.getAction().equalsIgnoreCase("reject")) {
            roleChangeRequest.setRespondedAt(LocalDateTime.now());
            roleChangeRequest.setHandledBy(admin);
            roleChangeRequest.setRequestStatus(RequestStatus.REJECTED);
            roleChangeRequestRepository.save(roleChangeRequest);

        }
        else {
            throw new InvalidResponseForRoleChangeRequest("Invalid action");
        }

        return toDto(roleChangeRequest);
    }

    // ----------------------------------------------HELPER METHODS-----------------------------------------------------

    private RoleChangeRequestResponse toDto(RoleChangeRequest request) {
        RoleChangeRequestResponse dto = new RoleChangeRequestResponse();

        dto.setRequestId(request.getRequestId());
        dto.setUserName(request.getUser().getFirstName() + " " + request.getUser().getLastName());
        dto.setUserEmail(request.getUser().getEmail());
        dto.setRequestedRole(request.getRequestedRole());
        dto.setRequestStatus(request.getRequestStatus());
        dto.setRequestReason(request.getRequestReason());
        dto.setRequestedAt(request.getRequestedAt());

        if(request.getHandledBy() != null) {
            dto.setRespondedAt(request.getRespondedAt());
            dto.setAdminName(request.getHandledBy().getFirstName() + " " + request.getHandledBy().getLastName());
            dto.setAdminEmail(request.getHandledBy().getEmail());
        }

        return dto;
    }

}
