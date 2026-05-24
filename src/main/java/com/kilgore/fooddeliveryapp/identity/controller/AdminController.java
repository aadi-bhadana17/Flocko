package com.kilgore.fooddeliveryapp.identity.controller;

import com.kilgore.fooddeliveryapp.identity.dto.request.RoleChangeRequestDecisionDto;
import com.kilgore.fooddeliveryapp.identity.dto.request.UserRestrictionDto;
import com.kilgore.fooddeliveryapp.ordering.dto.response.OrderResponse;
import com.kilgore.fooddeliveryapp.identity.dto.response.RoleChangeRequestResponse;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserExtendedSummary;
import com.kilgore.fooddeliveryapp.identity.service.AdminService;
import com.kilgore.fooddeliveryapp.ordering.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasAnyAuthority('ADMIN')")
@RequestMapping("/api/admin")

public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;

    public AdminController(AdminService adminService, OrderService orderService) {
        this.adminService = adminService;
        this.orderService = orderService;
    }

    // --------------------------------------------FETCHING USERS ENDPOINT---------------------------------------------

    @GetMapping("/users")
    public List<UserExtendedSummary> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/users/{userId}")
    public UserExtendedSummary getUserById(@PathVariable Long userId) {
        return adminService.getUserById(userId);
    }

    // --------------------------------------------USER MANAGEMENT ENDPOINTS--------------------------------------------

    @PutMapping("/users/{userId}/restrict")
    public String restrictUser(@PathVariable Long userId, @RequestBody UserRestrictionDto request) {
        return adminService.restrictUser(userId, request);
    }

    @PutMapping("/users/{userId}/unrestrict")
    public String unrestrictUser(@PathVariable Long userId) {
        return adminService.unrestrictUser(userId);
    }

    @PutMapping("/users/{userId}/block")
    public String blockUser(@PathVariable Long userId) {
        return adminService.blockUser(userId);
    }

    @PutMapping("/users/{userId}/unblock")
    public String unblockUser(@PathVariable Long userId) {
         return adminService.unblockUser(userId);
    }

    @DeleteMapping("/users/{userId}")
    public String deleteUser(@PathVariable Long userId) {
        return adminService.deleteUser(userId);
    }

    // --------------------------------------------ROLE MANAGEMENT ENDPOINTS--------------------------------------------

    @GetMapping("/role-requests")
    public List<RoleChangeRequestResponse> getRoleRequests() {
        return adminService.getAllRoleRequests();
    }

    @PutMapping("/role-requests/{requestId}")
    public RoleChangeRequestResponse updateRoleRequest(@RequestBody RoleChangeRequestDecisionDto request,
                                                       @PathVariable Long requestId) {
        return adminService.updateRole(request, requestId);
    }

    // ------------------------------------------RESTAURANT MANAGEMENT ENDPOINTS----------------------------------------

    @PutMapping("/restaurants/{restaurantId}/suspend")
    public String suspendRestaurant(@PathVariable Long restaurantId) {
        return adminService.suspendRestaurant(restaurantId);
    }

    @PutMapping("/restaurants/{restaurantId}/activate")
    public String activateRestaurant(@PathVariable Long restaurantId) {
        return adminService.activateRestaurant(restaurantId);
    }

    // ---------------------------------------------ORDER MANAGEMENT ENDPOINT------------------------------------------

    @GetMapping("/orders")
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }
}
