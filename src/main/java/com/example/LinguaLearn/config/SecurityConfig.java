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
            new AntPathRequestMatcher("/"), // Main page served by controller
            new AntPathRequestMatcher("/login.html"), // If you have a separate login page
            new AntPathRequestMatcher("/favicon.ico"),
            // Correctly ignore static resources in subdirectories
            new AntPathRequestMatcher("/css/**"),
            new AntPathRequestMatcher("/js/**"),
            // new AntPathRequestMatcher("/images/**"), // Add other static resource folders if needed
            new AntPathRequestMatcher("/error"),
            new AntPathRequestMatcher("/public/**"),
            new AntPathRequestMatcher("/api/firebase/config") // Firebase config endpoint
            // Add any other paths needed for the login process that should bypass security
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Authorize requests AFTER ignoring the paths defined above
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    // Allow access to specific non-secured API endpoints if needed
                    // .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/api/secure/**").authenticated()
                    // You might want to allow access to certain pages without login
                    // .requestMatchers("/quiz", "/translate", "/words").permitAll()
                    .anyRequest().authenticated() // Secure everything else by default
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
