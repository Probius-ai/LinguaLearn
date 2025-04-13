package com.example.LinguaLearn.config;

import com.example.LinguaLearn.filter.FirebaseTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private FirebaseTokenFilter firebaseTokenFilter;

    // Define paths to be ignored by Spring Security completely
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
            new AntPathRequestMatcher("/"), // Root path for static index.html or welcome page
            new AntPathRequestMatcher("/login.html"),
            new AntPathRequestMatcher("/favicon.ico"),
            new AntPathRequestMatcher("/*.js"),
            new AntPathRequestMatcher("/*.css"),
            new AntPathRequestMatcher("/error"),
            new AntPathRequestMatcher("/public/**"),
            new AntPathRequestMatcher("/api/firebase/config") // <<< Explicitly ignore the config endpoint
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Authorize requests AFTER ignoring the paths defined above
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    // Secure API endpoints require authentication
                    .requestMatchers("/api/secure/**").authenticated()
                    // Any other request not ignored above must be authenticated
                    .anyRequest().authenticated()
            )
            .sessionManagement(sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable()); // Disable CSRF for stateless API

        // Add the Firebase token filter before the standard authentication filter
        // This filter will only run for requests NOT ignored by webSecurityCustomizer
        http.addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
