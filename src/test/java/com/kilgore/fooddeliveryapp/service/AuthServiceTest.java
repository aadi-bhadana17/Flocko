package com.kilgore.fooddeliveryapp.service;

import com.kilgore.fooddeliveryapp.common.security.JwtService;
import com.kilgore.fooddeliveryapp.identity.dto.request.LoginRequest;
import com.kilgore.fooddeliveryapp.identity.dto.request.SignupRequest;
import com.kilgore.fooddeliveryapp.identity.dto.response.LoginAuthResponse;
import com.kilgore.fooddeliveryapp.common.exceptions.InvalidCredentialsException;
import com.kilgore.fooddeliveryapp.common.exceptions.UserAlreadyExistsException;
import com.kilgore.fooddeliveryapp.common.exceptions.UserStatusException;
import com.kilgore.fooddeliveryapp.identity.service.AuthService;
import com.kilgore.fooddeliveryapp.identity.model.AccountStatus;
import com.kilgore.fooddeliveryapp.identity.model.User;
import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import com.kilgore.fooddeliveryapp.identity.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setup() {
        authService = new AuthService(userRepository, authenticationManager, jwtService, passwordEncoder);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_throwsWhenEmailAlreadyExists() {
        SignupRequest request = new SignupRequest("Test", "User", "test@example.com", "password1", "password1");
        when(userRepository.findByEmail("test@example.com")).thenReturn(new User());

        assertThrows(UserAlreadyExistsException.class, () -> authService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_savesUserAndReturnsLoginResponse() {
        SignupRequest request = new SignupRequest("Test", "User", "test@example.com", "password1", "password1");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword(),
                AuthorityUtils.NO_AUTHORITIES
        );
        User repoUser = createUser(1L, request.getEmail());

        when(userRepository.findByEmail(request.getEmail())).thenReturn(null, repoUser);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(authentication)).thenReturn("token");
        when(jwtService.getExpiresAt("token")).thenReturn(123L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginAuthResponse response = authService.registerUser(request);

        assertEquals("token", response.getToken());
        assertEquals("Test", response.getFirstName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(UserRole.CUSTOMER, response.getRole());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        List<User> savedUsers = captor.getAllValues();
        assertEquals(UserRole.CUSTOMER, savedUsers.get(0).getRole());
        assertEquals("encoded", savedUsers.get(0).getPassword());
        assertTrue(savedUsers.get(1).isOnline());
    }

    @Test
    void login_throwsWhenCredentialsInvalid() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("test@example.com", "password1")));
        verify(userRepository, never()).findByEmail(any(String.class));
    }

    @Test
    void login_throwsWhenUserBlocked() {
        User user = createUser(1L, "test@example.com");
        user.setAccountStatus(AccountStatus.BLOCKED);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                "password1",
                AuthorityUtils.NO_AUTHORITIES
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);

        assertThrows(UserStatusException.class,
                () -> authService.login(new LoginRequest(user.getEmail(), "password1")));
    }

    @Test
    void login_throwsWhenUserDeleted() {
        User user = createUser(1L, "test@example.com");
        user.setAccountStatus(AccountStatus.DELETED);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                "password1",
                AuthorityUtils.NO_AUTHORITIES
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);

        assertThrows(UserStatusException.class,
                () -> authService.login(new LoginRequest(user.getEmail(), "password1")));
    }

    @Test
    void login_returnsTokenAndSetsOnline() {
        User user = createUser(1L, "test@example.com");
        user.setAccountStatus(AccountStatus.ACTIVE);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                "password1",
                AuthorityUtils.NO_AUTHORITIES
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        when(jwtService.generateToken(authentication)).thenReturn("token");
        when(jwtService.getExpiresAt("token")).thenReturn(456L);

        LoginAuthResponse response = authService.login(new LoginRequest(user.getEmail(), "password1"));

        assertEquals("token", response.getToken());
        assertEquals(user.getEmail(), response.getEmail());
        assertTrue(user.isOnline());
        verify(userRepository).save(user);
    }

    @Test
    void logoutUser_setsUserOffline() {
        User user = createUser(1L, "test@example.com");
        user.setOnline(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                AuthorityUtils.NO_AUTHORITIES
        ));
        SecurityContextHolder.setContext(context);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);

        String response = authService.logoutUser();

        assertEquals("You have been logged out successfully.", response);
        assertTrue(!user.isOnline());
        verify(userRepository).save(user);
    }

    private User createUser(Long id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setRole(UserRole.CUSTOMER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setOnline(false);
        return user;
    }
}
