package com.example.LinguaLearn.controller;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.FirestoreService;
import com.example.LinguaLearn.service.GeminiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/wrong-sentences")
public class WrongProblemController {

    private static final Logger logger = LoggerFactory.getLogger(WrongProblemController.class);

    @Autowired
    private FirestoreService firestoreService;

    @Autowired
    private GeminiService geminiService;

    @GetMapping
    public String wrongNote(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login?redirect=/wrong-sentences";
        }

        try {
            List<String> wrongSentences = firestoreService.getWrongProblem(user.getUid());
            logger.info("Retrieved wrong sentences for user {}: {}", user.getUid(), wrongSentences);

            model.addAttribute("wrongSentences", wrongSentences);
            model.addAttribute("user", user);
            model.addAttribute("isLoggedIn", true);
            // Explicitly set error to avoid null evaluation in Thymeleaf
            model.addAttribute("error", null);
            return "wrongNoteView";
        } catch (Exception e) {
            logger.error("Error retrieving wrong sentences", e);
            model.addAttribute("error", "오답 노트를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("wrongSentences", List.of());
            return "wrongNoteView";
        }
    }

    @GetMapping("/retry")
    public String retryWrongSentence(@RequestParam String originalSentence, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login?redirect=/wrong-sentences";
        }

        model.addAttribute("testSentence", originalSentence);
        model.addAttribute("isRetry", true);
        model.addAttribute("language", "english"); // Default language
        model.addAttribute("level", "beginner");   // Default level
        model.addAttribute("user", user);
        model.addAttribute("isLoggedIn", true);
        // Add these attributes to avoid null checks in Thymeleaf
        model.addAttribute("loginPrompt", false);
        model.addAttribute("error", null);

        return "sentence-test";
    }

    @PostMapping("/check-retry")
    @ResponseBody
    public ResponseEntity<?> checkRetryTranslation(
            @RequestParam String originalSentence,
            @RequestParam String userTranslation,
            @RequestParam String targetLanguage,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "로그인이 필요합니다"));
        }

        try {
            // Use Gemini to check the translation quality
            String prompt = String.format(
                    "Evaluate if this translation from English to %s is correct: " +
                            "Original: \"%s\", Translation: \"%s\". " +
                            "Respond with just one of these words: CORRECT or INCORRECT, " +
                            "followed by a brief explanation of no more than 20 words.",
                    targetLanguage, originalSentence, userTranslation
            );

            String evaluationResult = geminiService.getContents(prompt);
            boolean isCorrect = evaluationResult.toUpperCase().startsWith("CORRECT");

            // Extract just the explanation part
            String explanation = "";
            if (evaluationResult.contains(" ")) {
                explanation = evaluationResult.substring(evaluationResult.indexOf(" ")).trim();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("correct", isCorrect);
            response.put("explanation", explanation);
            response.put("nextSentence", geminiService.generateSentenceForTest(targetLanguage, "beginner"));
            response.put("isRetry", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error checking retry translation", e);
            return ResponseEntity.badRequest().body(Map.of("error", "번역 확인 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}