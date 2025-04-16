package com.example.LinguaLearn.config;

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

import com.example.LinguaLearn.filter.FirebaseTokenFilter;

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
                new AntPathRequestMatcher("/images/**"), // Add other static resource folders if needed
                new AntPathRequestMatcher("/error"),
                new AntPathRequestMatcher("/public/**"),
                new AntPathRequestMatcher("/api/firebase/config"), // Firebase config endpoint
                new AntPathRequestMatcher("/api/auth/status") // Auth status endpoint
                // Add any other paths needed for the login process that should bypass security
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/api/secure/**").authenticated()
                                .anyRequest().permitAll() // 명시적으로 허용하지 않은 요청은 모두 인증 필요
                )
                .sessionManagement(sessionManagement ->
                        // IF_REQUIRED로 설정하여 세션 유지
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf.disable())
                // Add custom logout configuration
                .logout(logout ->
                        logout
                                .logoutUrl("/logout")
                                .logoutSuccessUrl("/")
                                .invalidateHttpSession(true)
                                .clearAuthentication(true)
                                .permitAll()
                );

        http.addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}