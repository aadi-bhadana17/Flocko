package com.kilgore.fooddeliveryapp.common.config;

import com.kilgore.fooddeliveryapp.common.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtTokenValidator extends OncePerRequestFilter {


    private final JwtService jwtService;

    public JwtTokenValidator(JwtService jwtService) {
        this.jwtService = jwtService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(7);

            String email = jwtService.extractEmail(token);
            String authorities = jwtService.extractAuthorities(token);

            if(email != null && !jwtService.isTokenExpired(token)){
                Authentication authentication = new UsernamePasswordAuthenticationToken(email,
                                        null, // we didn't pass credentials here, because jwt already proved identity
                        Collections.singleton(new SimpleGrantedAuthority(authorities)));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        catch (Exception e) {
            throw new BadCredentialsException("Invalid Token" + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
