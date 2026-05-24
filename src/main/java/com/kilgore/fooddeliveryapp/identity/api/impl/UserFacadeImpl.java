package com.kilgore.fooddeliveryapp.identity.api.impl;

import com.kilgore.fooddeliveryapp.identity.api.UserFacade;
import com.kilgore.fooddeliveryapp.identity.dto.summary.AddressSummary;
import com.kilgore.fooddeliveryapp.identity.dto.summary.UserSummary;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UserFacadeImpl implements UserFacade {
    
    private final UserRepository userRepository;

    public UserFacadeImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserSummary getUserById(Long userId) {
        // Implementation
        return null;
    }
    
    @Override
    public boolean isUserActive(Long userId) {
        // Implementation
        return false;
    }

    @Override
    public AddressSummary getDefaultAddress(Long userId) {
        return null;
    }

    @Override
    public void deductWalletBalance(Long userId, BigDecimal amount) {

    }

    // ... other methods
}