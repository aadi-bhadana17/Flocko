package com.kilgore.fooddeliveryapp.identity.service;

import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerUserDetailService implements UserDetailsService {


    private final UserRepository userRepository;

    public CustomerUserDetailService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("User with the username " + username + " not found");
        }

        UserRole role = user.getRole();
        if (role == null) {
            user.setRole(UserRole.CUSTOMER);
        }
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        assert role != null;
        grantedAuthorities.add(new SimpleGrantedAuthority(role.toString()));


        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), grantedAuthorities);
    }
}
