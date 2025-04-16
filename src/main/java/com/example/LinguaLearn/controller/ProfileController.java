package com.example.LinguaLearn.controller;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.UserService;
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

    @GetMapping
    public String showProfile(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login?redirect=/profile";
        }

        model.addAttribute("user", user);
        return "profile-page";
    }

    @PostMapping("/settings")
    public String updateSettings(
            @ModelAttribute User updatedUser,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login?redirect=/profile";
        }

        try {
            // Update user settings
            User updated = userService.updateUserSettings(user.getUid(), updatedUser);

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