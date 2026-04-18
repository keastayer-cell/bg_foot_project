package com.footballstats.backend.config;

import com.footballstats.backend.security.JsonAccessDeniedHandler;
import com.footballstats.backend.security.JsonAuthEntryPoint;
import com.footballstats.backend.security.PasswordChangeRequiredFilter;
import com.footballstats.backend.security.JwtAuthenticationFilter;
import com.footballstats.backend.security.ApiAccessRuleFilter;
import com.footballstats.backend.security.ApiRequestLoggingFilter;
import com.footballstats.backend.security.AuthRateLimitFilter;
import com.footballstats.backend.security.SecurityHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Локальный userDetailsService не используется.");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        SecurityHeadersFilter securityHeadersFilter,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        AuthRateLimitFilter authRateLimitFilter,
        PasswordChangeRequiredFilter passwordChangeRequiredFilter,
        ApiAccessRuleFilter apiAccessRuleFilter,
        ApiRequestLoggingFilter apiRequestLoggingFilter,
        JsonAuthEntryPoint jsonAuthEntryPoint,
        JsonAccessDeniedHandler jsonAccessDeniedHandler
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(handler -> handler
                .authenticationEntryPoint(jsonAuthEntryPoint)
                .accessDeniedHandler(jsonAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/guest", "/api/auth/refresh", "/api/auth/logout", "/api/auth/password-reset/complete").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()
                .requestMatchers("/api/health", "/api/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("TEAM_REP", "SUPER_ADMIN", "REFEREE")
                .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("TEAM_REP", "SUPER_ADMIN", "REFEREE")
                .requestMatchers(HttpMethod.PATCH, "/api/**").hasAnyRole("TEAM_REP", "SUPER_ADMIN", "REFEREE")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("TEAM_REP", "SUPER_ADMIN", "REFEREE")
                .anyRequest().permitAll()
            )
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(passwordChangeRequiredFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(apiAccessRuleFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(apiRequestLoggingFilter, ApiAccessRuleFilter.class);

        return http.build();
    }
}
