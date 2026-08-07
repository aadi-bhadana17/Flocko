package com.kilgore.fooddeliveryapp.identity.service;

import com.kilgore.fooddeliveryapp.identity.dto.request.SignupRequest;
import com.kilgore.fooddeliveryapp.identity.dto.request.LoginRequest;
import com.kilgore.fooddeliveryapp.identity.dto.response.LoginAuthResponse;
import com.kilgore.fooddeliveryapp.common.exceptions.InvalidCredentialsException;
import com.kilgore.fooddeliveryapp.common.exceptions.UserAlreadyExistsException;
import com.kilgore.fooddeliveryapp.common.exceptions.UserStatusException;
import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import com.kilgore.fooddeliveryapp.common.security.JwtService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }


    public LoginAuthResponse registerUser(SignupRequest request) {
        if(userRepository.findByEmail(request.getEmail()) != null) {
            throw new UserAlreadyExistsException();
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // encoding password - bcrypt (one-way hash)
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(UserRole.CUSTOMER);

        userRepository.save(user);

        return login(new LoginRequest(
                user.getEmail(),
                request.getPassword()   // Because we need to pass plain password, user.getPass() is encoded
        ));  // once user saved to repo, it will be directed to log-in method to auto-login
    }

    public LoginAuthResponse login(@Valid LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    ));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(request.getEmail());

        if(user.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new UserStatusException("Your account is currently blocked. Please contact support for more information.");
        }
        if(user.getAccountStatus() == AccountStatus.DELETED){
            throw new UserStatusException("Your account has been deleted. Please contact support for more information.");
        }

        String token = jwtService.generateToken(authentication);

        user.setOnline(true);
        userRepository.save(user);

        return new LoginAuthResponse(
                token,
                user.getFirstName(),
                user.getEmail(),
                user.getRole(),
                jwtService.getExpiresAt(token)
        );
    }

    @Transactional
    public String logoutUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        user.setOnline(false);

        System.out.println("Logging out user: " + email);

        userRepository.save(user);
        return "You have been logged out successfully.";
    }
}
