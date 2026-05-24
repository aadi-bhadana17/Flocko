package com.kilgore.fooddeliveryapp.common.util;

import com.kilgore.fooddeliveryapp.common.exceptions.EntityNotFoundException;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserAuthorization {

    private final UserRepository userRepository;

    public UserAuthorization(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public User authorizeUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Auth before check: " + authentication);
        if (authentication == null) {
            throw new AccessDeniedException("No valid authority");
        } else if(!authentication.isAuthenticated()) throw new AccessDeniedException("No valid authority");

        System.out.println("Auth: " + authentication);

        User user = userRepository.findByEmail(authentication.getName());

        if (user == null) throw new EntityNotFoundException("User not found");
        return user;
    }
}
