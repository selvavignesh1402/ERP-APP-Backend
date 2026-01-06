package com.riceerp.backend.security;

import com.riceerp.backend.user.User;
import com.riceerp.backend.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collections;


import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public JwtFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

   @Override
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {

        String token = authHeader.substring(7);
        Long userId = JwtUtil.extractUserId(token);

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 🔑 SET AUTHENTICATION (THIS WAS MISSING)
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 🔒 Profile enforcement
        if (!user.isProfileCompleted()
                && !request.getRequestURI().startsWith("/profile")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
    }

    filterChain.doFilter(request, response);
}
}
