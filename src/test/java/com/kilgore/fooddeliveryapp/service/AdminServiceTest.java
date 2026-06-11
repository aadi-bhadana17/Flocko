package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.catalog.api.CatalogFacade;
import com.kilgore.fooddeliveryapp.catalog.dto.summary.RestaurantSummary;
import com.kilgore.fooddeliveryapp.identity.dto.request.RoleChangeRequestDecisionDto;
import com.kilgore.fooddeliveryapp.identity.dto.request.UserRestrictionDto;
import com.kilgore.fooddeliveryapp.identity.dto.response.RoleChangeRequestResponse;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserExtendedSummary;
import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.RequestStatus;
import com.kilgore.fooddeliveryapp.identity.model.RoleChangeRequest;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.repository.RoleChangeRequestRepository;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import com.kilgore.fooddeliveryapp.identity.service.AdminService;
import com.kilgore.fooddeliveryapp.identity.util.UserMapper;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.common.exceptions.InvalidResponseForRoleChangeRequest;
import com.kilgore.fooddeliveryapp.common.exceptions.RequestNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private RoleChangeRequestRepository roleChangeRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CatalogFacade catalogFacade;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminService adminService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_returnsMappedSummaries() {
        User owner = createUser(1L, "owner@example.com", UserRole.RESTAURANT_OWNER);
        owner.setOwnedRestaurantIds(Set.of(10L));
        UserExtendedSummary summary = UserExtendedSummary.builder()
                .id(1L)
                .name("Test User")
                .email("owner@example.com")
                .ownedRestaurants(List.of(new RestaurantSummary(10L, "Tasty Hub", null, null)))
                .build();

        when(userRepository.findAll()).thenReturn(List.of(owner));
        when(userMapper.toUserExtendedSummary(owner)).thenReturn(summary);

        List<UserExtendedSummary> responses = adminService.getAllUsers();

        assertEquals(1, responses.size());
        assertEquals("Test User", responses.get(0).getName());
        assertEquals(1, responses.get(0).getOwnedRestaurants().size());
        assertEquals(10L, responses.get(0).getOwnedRestaurants().get(0).getRestaurantId());
    }

    @Test
    void getUserById_returnsMappedSummary() {
        User user = createUser(2L, "user@example.com", UserRole.CUSTOMER);
        UserExtendedSummary summary = UserExtendedSummary.builder()
                .id(2L)
                .email("user@example.com")
                .role(UserRole.CUSTOMER)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userMapper.toUserExtendedSummary(user)).thenReturn(summary);

        UserExtendedSummary response = adminService.getUserById(2L);

        assertEquals(2L, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals(UserRole.CUSTOMER, response.getRole());
    }

    @Test
    void getUserById_throwsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.getUserById(99L));
    }

    @Test
    void restrictUser_updatesOwnerAndClosesRestaurants() {
        User owner = createUser(1L, "owner@example.com", UserRole.RESTAURANT_OWNER);
        owner.setOwnedRestaurantIds(Set.of(10L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String response = adminService.restrictUser(1L, new UserRestrictionDto(1L, "Violation", 2));

        assertEquals(AccountStatus.RESTRICTED, owner.getAccountStatus());
        assertNotNull(owner.getRestrictedUntil());
        assertEquals("Violation", owner.getRestrictionReason());
        assertTrue(response.contains("restricted"));
        verify(catalogFacade).suspendRestaurant(10L);
    }

    @Test
    void unrestrictUser_clearsRestrictionFields() {
        User user = createUser(1L, "user@example.com", UserRole.CUSTOMER);
        user.setAccountStatus(AccountStatus.RESTRICTED);
        user.setRestrictedUntil(LocalDateTime.now().plusDays(1));
        user.setRestrictionReason("Reason");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String response = adminService.unrestrictUser(1L);

        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        assertNull(user.getRestrictedUntil());
        assertNull(user.getRestrictionReason());
        assertTrue(response.contains("unrestricted"));
        verify(userRepository).save(user);
    }

    @Test
    void blockUser_suspendsOwnerRestaurants() {
        User owner = createUser(1L, "owner@example.com", UserRole.RESTAURANT_OWNER);
        owner.setOwnedRestaurantIds(Set.of(10L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        String response = adminService.blockUser(1L);

        assertEquals(AccountStatus.BLOCKED, owner.getAccountStatus());
        assertTrue(response.contains("blocked"));
        verify(catalogFacade).suspendRestaurant(10L);
        verify(userRepository).save(owner);
    }

    @Test
    void unblockUser_reopensOwnerRestaurants() {
        User owner = createUser(1L, "owner@example.com", UserRole.RESTAURANT_OWNER);
        owner.setOwnedRestaurantIds(Set.of(10L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        String response = adminService.unblockUser(1L);

        assertEquals(AccountStatus.ACTIVE, owner.getAccountStatus());
        assertTrue(response.contains("unblocked"));
        verify(catalogFacade).reactivateRestaurant(10L);
        verify(userRepository).save(owner);
    }

    @Test
    void deleteUser_marksUserDeleted() {
        User user = createUser(1L, "user@example.com", UserRole.CUSTOMER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String response = adminService.deleteUser(1L);

        assertEquals(AccountStatus.DELETED, user.getAccountStatus());
        assertTrue(response.contains("deleted"));
        verify(userRepository).save(user);
    }

    @Test
    void getAllRoleRequests_returnsMappedResponses() {
        User user = createUser(1L, "user@example.com", UserRole.CUSTOMER);
        RoleChangeRequest request = createRoleChangeRequest(22L, user, UserRole.RESTAURANT_OWNER);
        request.setRequestReason("I run a kitchen");
        request.setRequestStatus(RequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());

        when(roleChangeRequestRepository.findAll()).thenReturn(List.of(request));

        List<RoleChangeRequestResponse> responses = adminService.getAllRoleRequests();

        assertEquals(1, responses.size());
        assertEquals(22L, responses.get(0).getRequestId());
        assertEquals(RequestStatus.PENDING, responses.get(0).getRequestStatus());
    }

    @Test
    void updateRole_approvesRequestAndUpdatesUserRole() {
        User admin = createUser(10L, "admin@example.com", UserRole.ADMIN);
        User user = createUser(1L, "user@example.com", UserRole.CUSTOMER);
        RoleChangeRequest request = createRoleChangeRequest(22L, user, UserRole.RESTAURANT_OWNER);
        request.setUser(user);

        authenticateAs(admin.getEmail());
        when(roleChangeRequestRepository.findById(22L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(admin);

        RoleChangeRequestResponse response = adminService.updateRole(new RoleChangeRequestDecisionDto("approve"), 22L);

        assertEquals(RequestStatus.APPROVED, request.getRequestStatus());
        assertEquals(UserRole.RESTAURANT_OWNER, user.getRole());
        assertEquals("Test User", response.getAdminName());
        verify(roleChangeRequestRepository).save(request);
        verify(userRepository).save(user);
    }

    @Test
    void updateRole_rejectsRequestWithoutUpdatingUser() {
        User admin = createUser(10L, "admin@example.com", UserRole.ADMIN);
        User user = createUser(1L, "user@example.com", UserRole.CUSTOMER);
        RoleChangeRequest request = createRoleChangeRequest(22L, user, UserRole.RESTAURANT_OWNER);
        request.setUser(user);

        authenticateAs(admin.getEmail());
        when(roleChangeRequestRepository.findById(22L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(admin);

        RoleChangeRequestResponse response = adminService.updateRole(new RoleChangeRequestDecisionDto("reject"), 22L);

        assertEquals(RequestStatus.REJECTED, request.getRequestStatus());
        assertEquals("admin@example.com", response.getAdminEmail());
        verify(roleChangeRequestRepository).save(request);
        verify(userRepository, never()).save(user);
    }

    @Test
    void updateRole_throwsWhenActionInvalid() {
        User admin = createUser(10L, "admin@example.com", UserRole.ADMIN);
        User user = createUser(1L, "user@example.com", UserRole.CUSTOMER);
        RoleChangeRequest request = createRoleChangeRequest(22L, user, UserRole.RESTAURANT_OWNER);
        request.setUser(user);

        authenticateAs(admin.getEmail());
        when(roleChangeRequestRepository.findById(22L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(admin);

        assertThrows(InvalidResponseForRoleChangeRequest.class,
                () -> adminService.updateRole(new RoleChangeRequestDecisionDto("maybe"), 22L));
    }

    @Test
    void updateRole_throwsWhenRequestMissing() {
        authenticateAs("admin@example.com");
        when(roleChangeRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class,
                () -> adminService.updateRole(new RoleChangeRequestDecisionDto("approve"), 99L));
    }

    @Test
    void suspendRestaurant_delegatesToCatalogFacade() {
        when(catalogFacade.suspendRestaurant(5L)).thenReturn("Restaurant suspended");

        String response = adminService.suspendRestaurant(5L);

        assertEquals("Restaurant suspended", response);
        verify(catalogFacade).suspendRestaurant(5L);
    }

    @Test
    void activateRestaurant_delegatesToCatalogFacade() {
        when(catalogFacade.reactivateRestaurant(5L)).thenReturn("Restaurant activated");

        String response = adminService.activateRestaurant(5L);

        assertEquals("Restaurant activated", response);
        verify(catalogFacade).reactivateRestaurant(5L);
    }

    private void authenticateAs(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                email,
                null,
                AuthorityUtils.NO_AUTHORITIES
        ));
        SecurityContextHolder.setContext(context);
    }

    private User createUser(Long id, String email, UserRole role) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setOnline(false);
        return user;
    }

    private RoleChangeRequest createRoleChangeRequest(Long id, User user, UserRole requestedRole) {
        RoleChangeRequest request = new RoleChangeRequest();
        request.setRequestId(id);
        request.setUser(user);
        request.setRequestedRole(requestedRole);
        return request;
    }
}
