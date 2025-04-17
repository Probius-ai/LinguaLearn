package com.example.LinguaLearn.controller;

import com.example.LinguaLearn.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/friends")
public class FriendsViewController {

    private static final Logger logger = LoggerFactory.getLogger(FriendsViewController.class);

    @GetMapping
    public String friendsPage(Model model, HttpSession session) {
        // Get current user from session
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.info("Redirecting to login page since user is not authenticated");
            return "redirect:/login?redirect=/friends";
        }

        logger.info("Showing friends page for user: {}", user.getUid());
        model.addAttribute("user", user);
        return "friends";
    }
}