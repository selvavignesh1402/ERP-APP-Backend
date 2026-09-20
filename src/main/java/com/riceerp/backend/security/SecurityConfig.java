package com.riceerp.backend.security;

import com.riceerp.backend.repository.OrganizationMembershipRepository;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.service.PermissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final PermissionService permissionService;

    public SecurityConfig(UserRepository userRepository,
                          OrganizationMembershipRepository membershipRepository,
                          PermissionService permissionService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.permissionService = permissionService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/me").authenticated()
                        // Public auth endpoints
                        .requestMatchers("/auth/login-password", "/auth/signup-password",
                                "/auth/send-otp", "/auth/verify-otp", "/auth/firebase-login", "/error").permitAll()
                        
                        // Global platform governance (Strictly Master Admin only)
                        .requestMatchers("/users/**").hasAnyRole("PLATFORM_MASTER_ADMIN", "MASTER_ADMIN")
                        .requestMatchers("/api/master-admin/**").hasAnyRole("PLATFORM_MASTER_ADMIN", "MASTER_ADMIN")
                        
                        // Organization, modules and core ERP endpoints (authenticated; fine-grained access gated by @PreAuthorize)
                        .requestMatchers("/api/organizations/**").authenticated()
                        .requestMatchers("/dashboard/**").authenticated()
                        .requestMatchers("/customers/**").authenticated()
                        .requestMatchers("/suppliers/**").authenticated()
                        .requestMatchers("/products/**").authenticated()
                        .requestMatchers("/inventory/**").authenticated()
                        .requestMatchers("/purchases/**").authenticated()
                        .requestMatchers("/sales/**").authenticated()
                        .requestMatchers("/api/sales-orders/**").authenticated()
                        .requestMatchers("/api/deliveries/**").authenticated()
                        .requestMatchers("/payments/**").authenticated()
                        .requestMatchers("/invoices/**").authenticated()
                        .requestMatchers("/reconciliations/**").authenticated()
                        .requestMatchers("/beat-plans/**").authenticated()
                        .requestMatchers("/visits/**").authenticated()
                        
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtFilter(userRepository, membershipRepository, permissionService), UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*,http://192.168.*:*,http://10.*:*,exp://*}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList()));
        config.setAllowedHeaders(
                Arrays.asList("Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
