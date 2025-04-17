package com.example.LinguaLearn.controller;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.FriendService;
import com.example.LinguaLearn.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/profile")
public class ProfileViewController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileViewController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private FriendService friendService;

    /**
     * View another user's profile
     */
    @GetMapping("/view/{userId}")
    public String viewUserProfile(@PathVariable String userId, Model model, HttpSession session) {
        // Get current user from session
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            logger.info("Redirecting to login page since user is not authenticated");
            return "redirect:/login?redirect=/profile/view/" + userId;
        }

        // Check if trying to view own profile
        if (currentUser.getUid().equals(userId)) {
            return "redirect:/profile";
        }

        try {
            // Get user profile to view
            User profileUser = userService.getUser(userId);
            if (profileUser == null) {
                logger.warn("Requested user profile not found: {}", userId);
                return "redirect:/friends?error=userNotFound";
            }

            // Check friendship status
            boolean areFriends = friendService.areFriends(currentUser.getUid(), userId);

            model.addAttribute("currentUser", currentUser);
            model.addAttribute("profileUser", profileUser);
            model.addAttribute("areFriends", areFriends);

            return "profile-view";
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error retrieving user profile for: {}", userId, e);
            Thread.currentThread().interrupt();
            return "redirect:/friends?error=serverError";
        }
    }
}