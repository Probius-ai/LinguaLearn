package com.example.LinguaLearn.controller;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.security.FirebaseAuthenticationToken;
import com.example.LinguaLearn.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/secure/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/sync") // Endpoint to sync user data after login
    public ResponseEntity<?> syncUserProfile(HttpSession httpSession) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        // FirebaseToken 대신 FirebaseAuthenticationToken 또는 User 객체 사용
        String uid;
        String email = null;
        String name = null;
        
        if (authentication instanceof FirebaseAuthenticationToken) {
            FirebaseAuthenticationToken firebaseAuth = (FirebaseAuthenticationToken) authentication;
            uid = firebaseAuth.getUid();
            email = firebaseAuth.getEmail();
            name = firebaseAuth.getName();
        } else {
            uid = authentication.getName(); // 기본 인증에서는 getName()이 uid를 반환
        }
        
        logger.info("Syncing user profile for UID: {}", uid);

        try {
            // 필요한 사용자 데이터로 사용자 생성 또는 업데이트
            User user = userService.createOrUpdateUser(uid, email, name);
            
            // 세션에 사용자 저장
            httpSession.setAttribute("user", user);
            logger.info("User profile synced and stored in session: {}", user);
            return ResponseEntity.ok(user);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error creating/updating user profile in Firestore for UID: {}", uid, e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error accessing user profile: " + e.getMessage());
        }
    }
    
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpSession httpSession) {
        User user = (User) httpSession.getAttribute("user");
        if (user == null) {
            // 세션에 사용자가 없으면 SecurityContext에서 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                String uid = authentication.getName();
                try {
                    user = userService.getUser(uid);
                    if (user != null) {
                        // 세션에 사용자 정보 저장
                        httpSession.setAttribute("user", user);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("Error fetching user profile", e);
                    Thread.currentThread().interrupt();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching user profile");
                }
            }
        }
        
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
    }
}