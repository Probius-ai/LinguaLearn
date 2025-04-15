package com.example.LinguaLearn.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component // Register as a Spring component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseTokenFilter.class);

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 세션에서 인증 정보 확인
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            // 이미 인증된 세션이 있으면 처리
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = (User) session.getAttribute("user");
                logger.debug("Found authenticated user in session: {}", user.getUid());
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 기존 토큰 검증 로직
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = header.substring(7); // Remove "Bearer " prefix

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            logger.debug("Firebase token verified for UID: {}", uid);

            // UsernamePasswordAuthenticationToken 생성
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    decodedToken, null, new ArrayList<>());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // REST API 요청인 경우 세션에 사용자 정보 저장 (API 내부에서 처리되지 않은 경우)
            if (request.getRequestURI().startsWith("/api/") && !request.getRequestURI().equals("/api/secure/users/sync")) {
                try {
                    // 매번 동기화하지 않고 세션에 없을 때만 사용자 정보 가져오기
                    if (session == null || session.getAttribute("user") == null) {
                        session = request.getSession(true);
                        User user = userService.getUser(uid);
                        if (user != null) {
                            session.setAttribute("user", user);
                            logger.debug("User data stored in session for UID: {}", uid);
                        }
                    }
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("Error getting user data for UID: {}", uid, e);
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            // Token verification failed
            logger.error("Firebase token verification failed", e);
            SecurityContextHolder.clearContext(); // Clear context if token is invalid
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase token");
            return; // Stop filter chain execution
        }

        filterChain.doFilter(request, response);
    }
}
