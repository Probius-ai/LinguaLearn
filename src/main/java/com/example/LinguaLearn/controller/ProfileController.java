package com.example.LinguaLearn.controller;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.UserService;
import com.example.LinguaLearn.service.FirestoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private FirestoreService firestoreService;

@GetMapping
    public String showProfile(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        logger.info("start profile");
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login?redirect=/profile";
        }
        logger.info("Fetching user profile for UID: {}", user.getUid());
        logger.info("Fetching user profile for name: {}", user.getDisplayName());
        int level = firestoreService.getLevel(user.getUid());
        logger.info("Level {}", level);
        model.addAttribute("user", user);
        model.addAttribute("score", level);
        return "profile-page";
    }

    @PostMapping("/settings")
    public String updateSettings(
            @ModelAttribute User updatedUser, HttpSession session) {
        logger.info("start settings");
        logger.info("updated user: {}", updatedUser.getDisplayName());
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login?redirect=/profile";
        }
        logger.info("Updating user profile for UID: {}", user.getUid());

        try {
            // Update user settings
            User updated = userService.updateUserSettings(user.getUid(), updatedUser);
            logger.info("Updating user profile for Name: {}", updated.getDisplayName());
            // Update session
            session.setAttribute("user", updated);

            // Redirect back to profile with success message
            return "redirect:/profile?success=settings-updated";
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error updating user settings", e);
            Thread.currentThread().interrupt();
            return "redirect:/profile?error=settings-update-failed";
        }
    }

    @PostMapping("/api/theme")
    @ResponseBody
    public ResponseEntity<?> updateTheme(
            @RequestBody ThemeRequest request,
            HttpSession session) {

        // Store theme preference in session
        session.setAttribute("theme", request.getTheme());

        return ResponseEntity.ok().build();
    }

    // Helper class for theme request
    static class ThemeRequest {
        private String theme;

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }
    }
}
