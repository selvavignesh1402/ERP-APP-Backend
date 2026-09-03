package com.riceerp.backend.security;

import com.riceerp.backend.repository.UserRepository;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/me").authenticated()
                        // Only exact public auth endpoints — any new /auth/* endpoint
                        // added later must be authenticated by default, never public.
                        .requestMatchers("/auth/login-password", "/auth/signup-password",
                                "/auth/send-otp", "/auth/verify-otp", "/auth/firebase-login", "/error").permitAll()
                        .requestMatchers("/users/**").hasAnyRole("MASTER_ADMIN", "ADMIN")
                        .requestMatchers("/api/organizations/**").hasAnyRole("MASTER_ADMIN", "ADMIN")
                        .requestMatchers("/api/master-admin/**").hasRole("MASTER_ADMIN")
                        .requestMatchers("/dashboard/**")
                        .hasAnyRole("MASTER_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT", "SALES", "WAREHOUSE", "DELIVERY")
                        .requestMatchers("/customers/**").hasAnyRole("MASTER_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT", "SALES", "WAREHOUSE", "DELIVERY")
                        .requestMatchers("/payments/**").hasAnyRole("MASTER_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT", "SALES", "DELIVERY")
                        .requestMatchers("/sales/**").hasAnyRole("MASTER_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT", "SALES", "WAREHOUSE", "DELIVERY")
                        .requestMatchers("/api/sales-orders/**").hasAnyRole("MASTER_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT", "SALES", "WAREHOUSE", "DELIVERY")
                        .requestMatchers("/api/deliveries/**").hasAnyRole("MASTER_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT", "SALES", "WAREHOUSE", "DELIVERY")
                        .requestMatchers("/purchases/**").hasAnyRole("MASTER_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT", "WAREHOUSE")
                        .requestMatchers("/products/**")
                        .hasAnyRole("MASTER_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT", "SALES", "WAREHOUSE", "DELIVERY")
                        .requestMatchers("/suppliers/**")
                        .hasAnyRole("MASTER_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT", "SALES", "WAREHOUSE")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtFilter(userRepository), UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.setAllowedHeaders(
                Arrays.asList("Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
