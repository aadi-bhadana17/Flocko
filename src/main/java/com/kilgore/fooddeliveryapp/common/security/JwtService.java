package com.kilgore.fooddeliveryapp.common.security;

import com.kilgore.fooddeliveryapp.identity.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
public class JwtService {

    private static final String SECRET = "bXktc3VwZXItc2VjcmV0LWtleS1teS1zdXBlci1zZWNyZXQta2V5LTEyMzQ1Njc4OTA=";

    private static final long EXPIRATION_TIME = 86_400_000;

    public String generateToken(Authentication authentication) {
        String email = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = populateAuthorities(authorities);

        Map<String, Object> claims = new HashMap<>();

        claims.put("email", email);
        claims.put("authorities", role);

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .and()
                .signWith(getKey())
                .compact();
    }

    public String generateToken(String email,  UserRole role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("authorities", role);

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .and()
                .signWith(getKey())
                .compact();
    }


    private SecretKey getKey() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        StringBuilder sb = new StringBuilder();
        for (GrantedAuthority grantedAuthority : authorities) {
            sb.append(grantedAuthority.getAuthority());
        }
        return sb.toString();
    }

    public String extractEmail(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }


    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractAuthorities(String token) {
        Claims claims = extractClaims(token);
        return claims.get("authorities", String.class);
    }

    public boolean isTokenExpired(String token) {
        return System.currentTimeMillis() > getExpiresAt(token);
    }

    public long getExpiresAt(String token) {
        Claims claims = extractClaims(token);
        return claims.getExpiration().getTime();
    }
}
