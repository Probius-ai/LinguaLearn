package com.example.LinguaLearn.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.GeminiService;

import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private GeminiService geminiService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // 세션에서 사용자 정보 가져오기
        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = (user != null);
        
        // 로그인 상태를 모델에 추가 - 타임리프가 세션을 직접 읽을 수 있지만 명시적으로도 추가
        model.addAttribute("isLoggedIn", isLoggedIn);
        
        if (isLoggedIn) {
            logger.debug("User is logged in: {}", user.getDisplayName());
            // 필요한 경우 추가 사용자 데이터 모델에 추가
        } else {
            logger.debug("User is not logged in");
        }
        
        // TODO: 일일 추천 단어 등 추가 데이터 로드
        try {
            // Get today's recommendation for the homepage demo
            String language = "english"; // Default language
            String level = "beginner";   // Default level
            
            // If user is logged in, use their preferences
            if (isLoggedIn && user.getPrimaryLanguage() != null) {
                language = user.getPrimaryLanguage();
            }
            
            model.addAttribute("dailyWords", geminiService.getTodayWord(language, level));
            
        } catch (Exception e) {
            logger.error("Error loading daily word for homepage", e);
            // Continue with page loading even if this fails
        }
        
        // This returns the name of the Thymeleaf template file (without .html)
        return "main-page";
    }
    
    // 퀴즈 메인 페이지 매핑
    @GetMapping("/quiz")
    public String quizMain(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = (user != null);
        model.addAttribute("isLoggedIn", isLoggedIn);
        
        if (isLoggedIn) {
            model.addAttribute("username", user.getDisplayName());
        }
        
        return "quiz-main";
    }

    // Translation page redirect
    @GetMapping("/translate")
    public String translateRedirect() {
        return "redirect:/ai/translate";
    }
    
    // Daily word redirect
    @GetMapping("/words")
    public String dailyWordRedirect() {
        return "redirect:/ai/daily-word";
    }
}