package com.example.LinguaLearn.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // 로그아웃 전 사용자 정보 로깅 (디버깅용)
        if (session != null && session.getAttribute("user") != null) {
            logger.info("User logged out: {}", session.getAttribute("user").toString());
        }
        
        // 세션 무효화
        if (session != null) {
            session.invalidate();
        }
        
        // 메인 페이지로 리디렉션
        return "redirect:/";
    }
    
    @GetMapping("/api/auth/status")
    @ResponseBody
    public ResponseEntity<?> getAuthStatus(HttpSession session) {
        if (session != null && session.getAttribute("user") != null) {
            return ResponseEntity.ok(session.getAttribute("user"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("{'loggedIn': false, 'message': 'Not authenticated'}");
        }
    }
}
