package com.example.LinguaLearn.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.LinguaLearn.model.User;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String SESSION_USER_KEY = "user";
    private static final String FIREBASE_TOKEN_COOKIE = "firebaseToken";

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        
        // 로그아웃 전 사용자 정보 로깅 (디버깅용)
        if (session != null && session.getAttribute(SESSION_USER_KEY) != null) {
            logger.info("User logged out: {}", session.getAttribute(SESSION_USER_KEY).toString());
        }
        
        // 세션 무효화
        if (session != null) {
            session.invalidate();
        }
        
        // SecurityContext 클리어
        SecurityContextHolder.clearContext();
        
        // Firebase 토큰 쿠키 삭제 (있는 경우)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (FIREBASE_TOKEN_COOKIE.equals(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
        
        // 메인 페이지로 리디렉션
        return "redirect:/";
    }
    
    @GetMapping("/api/auth/status")
    @ResponseBody
    public ResponseEntity<?> getAuthStatus(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        // 세션에서 사용자 확인
        if (session != null && session.getAttribute(SESSION_USER_KEY) != null) {
            User user = (User) session.getAttribute(SESSION_USER_KEY);
            
            // 세션 접근 시간 갱신
            session.setAttribute("lastAccessTime", System.currentTimeMillis());
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", user.getUid());
            userData.put("email", user.getEmail());
            userData.put("displayName", user.getDisplayName());
            userData.put("authenticated", true);
            userData.put("sessionActive", true);
            userData.put("sessionId", session.getId());
            
            return ResponseEntity.ok(userData);
        }
        
        // SecurityContext에서 사용자 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof FirebaseToken) {
            FirebaseToken token = (FirebaseToken) auth.getPrincipal();
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", token.getUid());
            userData.put("email", token.getEmail());
            userData.put("displayName", token.getName());
            userData.put("authenticated", true);
            userData.put("sessionActive", false);
            
            return ResponseEntity.ok(userData);
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "loggedIn", false, 
                "message", "Not authenticated"
            ));
    }
    
    @GetMapping("/api/auth/session-info")
    @ResponseBody
    public ResponseEntity<?> getSessionInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Map<String, Object> info = new HashMap<>();
        
        if (session != null) {
            info.put("sessionId", session.getId());
            info.put("creationTime", session.getCreationTime());
            info.put("lastAccessedTime", session.getLastAccessedTime());
            info.put("maxInactiveInterval", session.getMaxInactiveInterval());
            info.put("isNew", session.isNew());
            
            // 세션에 저장된 마지막 접근 시간 확인
            Long lastAccessTime = (Long) session.getAttribute("lastAccessTime");
            if (lastAccessTime != null) {
                info.put("lastActivityTime", lastAccessTime);
                long timeElapsed = System.currentTimeMillis() - lastAccessTime;
                info.put("inactiveTime", timeElapsed); // 밀리초 단위
                info.put("inactiveSeconds", timeElapsed / 1000); // 초 단위
            }
            
            if (session.getAttribute(SESSION_USER_KEY) != null) {
                User user = (User) session.getAttribute(SESSION_USER_KEY);
                info.put("user", Map.of(
                    "uid", user.getUid(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName()
                ));
            } else {
                info.put("user", null);
            }
            
            return ResponseEntity.ok(info);
        }
        
        info.put("sessionExists", false);
        return ResponseEntity.ok(info);
    }
    
    @PostMapping("/api/auth/heartbeat")
    @ResponseBody
    public ResponseEntity<?> sessionHeartbeat(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "No active session"));
        }
        
        // 세션에 사용자가 있는지 확인
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "No authenticated user in session"));
        }
        
        // 세션 접근 시간 갱신
        session.setAttribute("lastAccessTime", System.currentTimeMillis());
        
        // 세션 타임아웃 갱신 (30분)
        session.setMaxInactiveInterval(1800);
        
        logger.debug("Session heartbeat for user: {}, session: {}", user.getUid(), session.getId());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "sessionId", session.getId(),
            "timeoutAt", System.currentTimeMillis() + (session.getMaxInactiveInterval() * 1000)
        ));
    }
}