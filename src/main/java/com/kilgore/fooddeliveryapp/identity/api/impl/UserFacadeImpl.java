package com.kilgore.fooddeliveryapp.identity.api.impl;

import com.kilgore.fooddeliveryapp.catalog.dto.request.AddStaffRequest;
import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.response.StaffCreationResponse;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.StaffSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserExtendedSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.repository.AddressRepository;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import com.kilgore.fooddeliveryapp.identity.util.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Component
public class UserFacadeImpl implements UserFacade {
    
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserFacadeImpl(UserRepository userRepository, AddressRepository addressRepository,
                          UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new EntityNotFoundException("User not found with email: " + email);
        }
        return user.getUserId();
    }

    @Override
    public UserSummary getUserById(Long userId) {
        User user = fetchUser(userId);
        return userMapper.toUserSummary(user);
    }

    @Override
    public UserExtendedSummary getUserExtendedById(Long userId) {
        User user = fetchUser(userId);
        return userMapper.toUserExtendedSummary(user);
    }

    @Override
    public boolean isUserActive(Long userId) {
        User user = fetchUser(userId);
        return user.getAccountStatus().equals(AccountStatus.ACTIVE);
    }

    @Override
    public boolean isUserRestricted(Long userId) {
        User user = fetchUser(userId);
        return user.getAccountStatus().equals(AccountStatus.RESTRICTED);
    }

    @Override
    public AddressSummary getDefaultAddress(Long userId) {
        User user = fetchUser(userId);
        Address address = addressRepository.getDefaultAddressByUserId(userId);
        if (address == null) {
            throw new EntityNotFoundException("Default Address not found");
        }
        return userMapper.toAddressSummary(address, user);
    }

    @Override
    public AddressSummary getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + addressId));
        User user = fetchUser(address.getUser().getUserId());
        return userMapper.toAddressSummary(address, user);
    }

    @Override
    public boolean isAddressOwnedByUser(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + addressId));
        return address.getUser() != null && userId.equals(address.getUser().getUserId());
    }

    @Override
    public BigDecimal getWalletBalance(Long userId) {
        User user = fetchUser(userId);
        return user.getWalletBalance();
    }

    @Override
    public void setWalletBalance(Long userId, BigDecimal balance) {
        User user = fetchUser(userId);
        user.setWalletBalance(balance);
        userRepository.save(user);
    }

    @Override
    public void addWalletBalance(Long userId, BigDecimal  amount) {
        User user = fetchUser(userId);
        user.setWalletBalance(getWalletBalance(userId).add(amount));
        userRepository.save(user);
    }

    @Override
    public void deductWalletBalance(Long userId, BigDecimal amount) {
        User user = fetchUser(userId);
        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }
        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        userRepository.save(user);
    }

    @Override
    public boolean isEmployedAt(Long userId, Long restaurantId) {
        User user = fetchUser(userId);
        return user.getEmployedAt() != null && user.getEmployedAt().equals(restaurantId);
    }

    @Override
    public boolean isUserRestaurantStaff(Long userId) {
        User user = fetchUser(userId);
        return user.getRole().equals(UserRole.RESTAURANT_STAFF);
    }

    @Override
    public boolean isUserRestaurantOwner(Long userId) {
        User user = fetchUser(userId);
        return user.getRole().equals(UserRole.RESTAURANT_OWNER);
    }

    @Override
    public boolean isUserAdmin(Long userId) {
        User user = fetchUser(userId);
        return user.getRole().equals(UserRole.ADMIN);
    }

    @Override
    public boolean isOwnerOfRestaurant(Long userId, Long restaurantId) {
        User user = fetchUser(userId);
        if (user.getOwnedRestaurantIds() == null || restaurantId == null) {
            return false;
        }
        return user.getOwnedRestaurantIds().stream()
                .anyMatch(ownedId -> ownedId.equals(restaurantId));
    }

    @Override
    public boolean isUserCustomer(Long userId) {
        User user = fetchUser(userId);
        return user.getRole().equals(UserRole.CUSTOMER);
    }

    @Override
    public LocalDateTime userRestrictedUntil(Long userId) {
        User user = fetchUser(userId);
        return user.getRestrictedUntil();
    }

    @Override
    public StaffCreationResponse createStaff(AddStaffRequest request, Long restaurantId) {
        String password = request.getFirstName() + "@" + request.getPhone().substring(7) + "***";

        User staff = new User();

        staff.setFirstName(request.getFirstName());
        staff.setLastName(request.getLastName());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        staff.setRole(UserRole.RESTAURANT_STAFF);
        staff.setEmployedAt(restaurantId);
        staff.setPassword(passwordEncoder.encode(password));
        staff.setTempPassword(true);

        userRepository.save(staff);

        return userMapper.toStaffCreationResponse(staff, password);
    }

    @Override
    public List<StaffSummary> getRestaurantStaff(Long restaurantId) {
        return userRepository.findByEmployedAt(restaurantId)
                .stream()
                .map(userMapper::toStaffSummary)
                .toList();
    }

    @Override
    public boolean canUserManageRestaurant(Long userId, Long restaurantId) {
        User user = fetchUser(userId);

        if (user.getRole() == UserRole.ADMIN)
            return true;

        if (user.getEmployedAt() != null && user.getEmployedAt().equals(restaurantId))
            return true;


        return isOwnerOfRestaurant(userId, restaurantId);
    }

    @Override
    public void checkDefaultAddress(Long userId) {
        if(addressRepository.getDefaultAddressByUserId(userId) == null)
            throw new EntityNotFoundException("Default Address not found");
    }


    private User fetchUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }


    // ... other methods
}