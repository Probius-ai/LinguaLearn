package com.example.LinguaLearn.controller;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.UserService;
// import com.google.firebase.auth.FirebaseAuth; // No longer needed here
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/secure/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/sync") // Endpoint to sync user data after login
    public ResponseEntity<?> syncUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof FirebaseToken)) {
            // Check if principal is actually a FirebaseToken
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated or invalid token principal");
        }

        FirebaseToken decodedToken = (FirebaseToken) authentication.getPrincipal(); // Cast principal to FirebaseToken
        String uid = decodedToken.getUid(); // Get UID from the token if needed for logging or other purposes

        try {
            // Directly use the decodedToken obtained from the Authentication context
            User user = userService.createOrUpdateUser(decodedToken);
            return ResponseEntity.ok(user);
        // Remove FirebaseAuthException catch block as we are not calling FirebaseAuth.getUser() anymore
        // } catch (FirebaseAuthException e) {
        //      logger.error("Error fetching user data from Firebase Auth for UID: {}", uid, e);
        //      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching Firebase user data: " + e.getMessage());
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error creating/updating user profile in Firestore for UID: {}", uid, e);
            Thread.currentThread().interrupt(); // Restore interrupt status
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error accessing user profile: " + e.getMessage());
        }
    }
}
