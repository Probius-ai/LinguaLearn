package com.example.LinguaLearn.controller;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class RankingController {

    private static final Logger logger = LoggerFactory.getLogger(RankingController.class);

    @Autowired
    private RankingService rankingService;

    @GetMapping("/ranking")
    public String showRankingPage(Model model, HttpSession session) {
        // Get current user from session
        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = (user != null);
        model.addAttribute("isLoggedIn", isLoggedIn);

        if (isLoggedIn) {
            model.addAttribute("user", user);
        }

        try {
            // Get top 10 user rankings
            List<Map<String, Object>> rankings = rankingService.getTopRankings(10);
            model.addAttribute("rankings", rankings);

            // Get user's own rank if logged in
            if (isLoggedIn) {
                Map<String, Object> userRank = rankingService.getUserRank(user.getUid());
                model.addAttribute("userRank", userRank);
            }

            return "ranking";
        } catch (Exception e) {
            logger.error("Error fetching ranking data", e);
            model.addAttribute("error", "랭킹 데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "ranking";
        }
    }
}