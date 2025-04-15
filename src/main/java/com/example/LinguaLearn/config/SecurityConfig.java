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

import com.example.LinguaLearn.filter.ConcurrentSessionFilter;
import com.example.LinguaLearn.filter.FirebaseTokenFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private FirebaseTokenFilter firebaseTokenFilter;
    
    @Autowired
    private ConcurrentSessionFilter concurrentSessionFilter;

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
            new AntPathRequestMatcher("/api/auth/status"), // Auth status endpoint
            new AntPathRequestMatcher("/h2-console/**") // H2 콘솔 접근 허용 (개발용)
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
                // 세션 설정 - 항상 세션 생성
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .maximumSessions(1) // 사용자당 최대 1개 세션 허용
                .maxSessionsPreventsLogin(false) // 이전 세션 만료 처리
            )
            .csrf(csrf -> csrf.disable());

        // 필터 순서 설정: ConcurrentSessionFilter를 먼저 실행하고 FirebaseTokenFilter 실행
        http.addFilterBefore(concurrentSessionFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(firebaseTokenFilter, ConcurrentSessionFilter.class);

        return http.build();
    }
}