package com.agrishield.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public authentication and actuator health
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // 1. RFID Scanning (FARMHAND, VETERINARIAN, MANAGER, AUDITOR)
                .requestMatchers(HttpMethod.GET, "/api/v1/animals/scan/**")
                    .hasAnyRole("FARMHAND", "VETERINARIAN", "MANAGER", "AUDITOR")

                // 2. Data Cleaner & Treatments (VETERINARIAN, MANAGER)
                .requestMatchers(HttpMethod.POST, "/api/v1/data/clean")
                    .hasAnyRole("VETERINARIAN", "MANAGER")
                .requestMatchers("/api/v1/treatments/**")
                    .hasAnyRole("VETERINARIAN", "MANAGER")

                // 3. Shipment Permit Gatekeeper (MANAGER only)
                .requestMatchers(HttpMethod.POST, "/api/v1/shipments/permit")
                    .hasRole("MANAGER")
                .requestMatchers("/api/v1/manager/**")
                    .hasRole("MANAGER")

                // 4. Auditor Blockchain Explorer (AUDITOR, MANAGER - Read Only)
                .requestMatchers(HttpMethod.GET, "/api/v1/auditor/**")
                    .hasAnyRole("AUDITOR", "MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/v1/auditor/**")
                    .denyAll() // State modification on auditor endpoints is strictly prohibited per spec

                // Any other request must be authenticated
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
