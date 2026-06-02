package com.kilgore.fooddeliveryapp.identity.api.impl;

import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.Address;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.repository.AddressRepository;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import com.kilgore.fooddeliveryapp.identity.util.UserMapper;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;

@Component
public class UserFacadeImpl implements UserFacade {
    
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserMapper userMapper;

    public UserFacadeImpl(UserRepository userRepository, AddressRepository addressRepository,
                          UserMapper userMapper) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.userMapper = userMapper;
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
        User user = getUser(userId);
        return userMapper.toUserSummary(user);
    }
    
    @Override
    public boolean isUserActive(Long userId) {
        User user = getUser(userId);
        return user.getAccountStatus().equals(AccountStatus.ACTIVE);
    }

    @Override
    public boolean isUserRestricted(Long userId) {
        User user = getUser(userId);
        return user.getAccountStatus().equals(AccountStatus.RESTRICTED);
    }

    @Override
    public AddressSummary getDefaultAddress(Long userId) {
        User user = getUser(userId);
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
        User user = getUser(address.getUser().getUserId());
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
        User user = getUser(userId);
        return user.getWalletBalance();
    }

    @Override
    public void setWalletBalance(Long userId, BigDecimal balance) {
        User user = getUser(userId);
        user.setWalletBalance(balance);
        userRepository.save(user);
    }

    @Override
    public void addWalletBalance(Long userId, BigDecimal  amount) {
        User user = getUser(userId);
        user.setWalletBalance(getWalletBalance(userId).add(amount));
        userRepository.save(user);
    }

    @Override
    public void deductWalletBalance(Long userId, BigDecimal amount) {
        User user = getUser(userId);
        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }
        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        userRepository.save(user);
    }

    @Override
    public boolean isEmployedAt(Long userId, Long restaurantId) {
        User user = getUser(userId);
        return user.getEmployedAt() != null && user.getEmployedAt().equals(restaurantId);
    }

    @Override
    public boolean isUserRestaurantStaff(Long userId) {
        User user = getUser(userId);
        return user.getRole().equals(UserRole.RESTAURANT_STAFF);
    }

    @Override
    public boolean isUserRestaurantOwner(Long userId) {
        User user = getUser(userId);
        return user.getRole().equals(UserRole.RESTAURANT_OWNER);
    }

    @Override
    public boolean isUserAdmin(Long userId) {
        User user = getUser(userId);
        return user.getRole().equals(UserRole.ADMIN);
    }

    @Override
    public boolean isOwnerOfRestaurant(Long userId, Long restaurantId) {
        User user = getUser(userId);
        if (user.getOwnedRestaurants() == null || restaurantId == null) {
            return false;
        }
        return user.getOwnedRestaurants().stream()
                .anyMatch(restaurant -> restaurantId.equals(restaurant.getRestaurantId()));
    }

    @Override
    public boolean isUserCustomer(Long userId) {
        User user = getUser(userId);
        return user.getRole().equals(UserRole.CUSTOMER);
    }


    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }


    // ... other methods
}