package com.example.LinguaLearn.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.GeminiService;
import com.example.LinguaLearn.service.UserService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/ai")
public class GeminiController {

    private static final Logger logger = LoggerFactory.getLogger(GeminiController.class);

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private UserService userService;

    @Autowired
    private Firestore firestore;

    @GetMapping("/translate")
    public String showTranslatePage(Model model, HttpSession session) {
        // Add any necessary attributes to the model
        User user = (User) session.getAttribute("user");
        model.addAttribute("isLoggedIn", user != null);

        return "translate";
    }

    @PostMapping("/translate")
    @ResponseBody
    public ResponseEntity<?> translateText(
            @RequestParam String text,
            @RequestParam String fromLanguage,
            @RequestParam String toLanguage) {

        try {
            String translatedText = geminiService.translateText(text, fromLanguage, toLanguage);
            return ResponseEntity.ok(Map.of("translation", translatedText));
        } catch (Exception e) {
            logger.error("Translation error", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Translation failed: " + e.getMessage()));
        }
    }

    @GetMapping("/daily-word")
    public String showDailyWordPage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = (user != null);
        model.addAttribute("isLoggedIn", isLoggedIn);

        try {
            String language = "english"; // Default language
            String level = "beginner";   // Default level

            // If user is logged in, use their preferences
            if (isLoggedIn && user.getPrimaryLanguage() != null) {
                language = user.getPrimaryLanguage();
            }

            Map<String, String> wordData = geminiService.getTodayWord(language, level);

            // wordData가 null인 경우 처리
            if (wordData == null) {
                model.addAttribute("hasError", true); // Using hasError instead of error
                model.addAttribute("errorMessage", "오늘의 단어를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.");
                return "daily-word";
            }

            model.addAttribute("wordData", wordData);
            model.addAttribute("language", language);
            // Always set hasError attribute
            model.addAttribute("hasError", false);

        } catch (Exception e) {
            logger.error("Error fetching daily word", e);
            model.addAttribute("hasError", true);
            model.addAttribute("errorMessage", "단어 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "daily-word";
    }

    @GetMapping("/sentence-test")
    public String showSentenceTestPage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = (user != null);
        model.addAttribute("isLoggedIn", isLoggedIn);

        if (!isLoggedIn) {
            model.addAttribute("loginPrompt", true);
            model.addAttribute("error", null); // Explicitly set error to avoid null checks
            return "sentence-test";
        }

        try {
            logger.info("try block 실행");
            String language = "english"; // Default language
            String level = "beginner";   // Default level

            // If user has preferences, use them
            if (user.getPrimaryLanguage() != null) {
                language = user.getPrimaryLanguage();
            }
            logger.info("service 실행");
            String testSentence = geminiService.generateSentenceForTest(language, level);
            logger.info("service 실행 완료 : {}", testSentence);
            model.addAttribute("testSentence", testSentence);
            model.addAttribute("language", language);
            model.addAttribute("level", level);
            model.addAttribute("loginPrompt", false); // Explicitly set to false
            model.addAttribute("error", null); // Explicitly set to null
            model.addAttribute("isRetry", false); // Explicitly set for retry feature

        } catch (Exception e) {
            logger.error("Error generating sentence test", e);
            model.addAttribute("error", "Could not generate a test sentence. Please try again later.");
            model.addAttribute("loginPrompt", false); // Explicitly set
            model.addAttribute("isRetry", false); // Explicitly set
        }
        return "sentence-test";
    }

    @PostMapping("/check-translation")
    @ResponseBody
    public ResponseEntity<?> checkTranslation(
            @RequestParam String originalSentence,
            @RequestParam String userTranslation,
            @RequestParam String targetLanguage,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "You must be logged in to submit translations"));
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

            // Store the result in Firestore
            String uid = user.getUid();
            DocumentReference progressRef = firestore.collection("progress").document(uid);

            ApiFuture<DocumentSnapshot> future = progressRef.get();
            DocumentSnapshot document = future.get();

            Map<String, Object> updateData = new HashMap<>();
            if (document.exists()) {
                // Update existing document
                Long totalCnt = document.getLong("total_cnt") != null ? document.getLong("total_cnt") + 1 : 1;
                Long correctCnt = document.getLong("correct_cnt") != null ? document.getLong("correct_cnt") : 0;
                Long wrongCnt = document.getLong("wrong_cnt") != null ? document.getLong("wrong_cnt") : 0;

                List<Map<String, String>> correctSentences = (List<Map<String, String>>) document.get("correct_sentence");
                List<Map<String, String>> wrongSentences = (List<Map<String, String>>) document.get("wrong_sentence");

                if (correctSentences == null) correctSentences = new ArrayList<>();
                if (wrongSentences == null) wrongSentences = new ArrayList<>();

                Map<String, String> sentenceMap = new HashMap<>();
                sentenceMap.put("sentence", originalSentence);
                sentenceMap.put("translation", userTranslation);

                if (isCorrect) {
                    correctCnt++;
                    correctSentences.add(sentenceMap);
                } else {
                    wrongCnt++;
                    wrongSentences.add(sentenceMap);
                }

                updateData.put("total_cnt", totalCnt);
                updateData.put("correct_cnt", correctCnt);
                updateData.put("wrong_cnt", wrongCnt);
                updateData.put("correct_sentence", correctSentences);
                updateData.put("wrong_sentence", wrongSentences);
                updateData.put("last_submission_date", new Date());
            } else {
                // Create new document
                updateData.put("total_cnt", 1L);
                updateData.put("correct_cnt", isCorrect ? 1L : 0L);
                updateData.put("wrong_cnt", isCorrect ? 0L : 1L);

                List<Map<String, String>> sentences = new ArrayList<>();
                Map<String, String> sentenceMap = new HashMap<>();
                sentenceMap.put("sentence", originalSentence);
                sentenceMap.put("translation", userTranslation);

                if (isCorrect) {
                    updateData.put("correct_sentence", List.of(sentenceMap));
                    updateData.put("wrong_sentence", new ArrayList<>());
                } else {
                    updateData.put("correct_sentence", new ArrayList<>());
                    updateData.put("wrong_sentence", List.of(sentenceMap));
                }

                updateData.put("last_submission_date", new Date());
            }

            progressRef.set(updateData, SetOptions.merge());

            // Extract just the explanation part
            String explanation = "";
            if (evaluationResult.contains(" ")) {
                explanation = evaluationResult.substring(evaluationResult.indexOf(" ")).trim();
            }
            logger.info("targetLanguage : {}", targetLanguage);
            String nextSentence = geminiService.generateSentenceForTest(targetLanguage, "beginner");
            logger.info("nextSentence : {}", nextSentence);
            Map<String, Object> response = new HashMap<>();
            response.put("correct", isCorrect);
            response.put("explanation", explanation);
            response.put("nextSentence", nextSentence);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error checking translation", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to check translation: " + e.getMessage()));
        }
    }

    @GetMapping("/analyze-progress")
    public String analyzeProgress(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = (user != null); // 로그인 여부 판단
        model.addAttribute("isLoggedIn", isLoggedIn); // 모델에 추가!!

        if (!isLoggedIn) {
            return "redirect:/login?redirect=/ai/analyze-progress";
        }


        try {
            String uid = user.getUid();
            DocumentReference progressRef = firestore.collection("progress").document(uid);

            ApiFuture<DocumentSnapshot> future = progressRef.get();
            DocumentSnapshot document = future.get();

            if (!document.exists()) {
                model.addAttribute("noData", true);
                model.addAttribute("error", null); // Explicitly set to avoid null checks
                return "progress-analysis";
            }

            Long totalCount = document.getLong("total_cnt");
            Long correctCount = document.getLong("correct_cnt");
            List<Map<String, String>> correctSentences = (List<Map<String, String>>) document.get("correct_sentence");
            List<Map<String, String>> wrongSentences = (List<Map<String, String>>) document.get("wrong_sentence");

            // Prepare data for the analysis
            List<String> correctTexts = new ArrayList<>();
            if (correctSentences != null) {
                for (Map<String, String> entry : correctSentences) {
                    correctTexts.add(entry.get("sentence"));
                }
            }

            List<String> wrongTexts = new ArrayList<>();
            if (wrongSentences != null) {
                for (Map<String, String> entry : wrongSentences) {
                    wrongTexts.add(entry.get("sentence"));
                }
            }

            // Get AI analysis if there's enough data
            String analysis = "Not enough data to provide meaningful analysis.";
            if ((correctTexts.size() + wrongTexts.size()) >= 3) {
                analysis = geminiService.analyzeLearningProgress(correctTexts, wrongTexts);
            }
            logger.info("correctTexts : {}", correctSentences);
            logger.info("wrongTexts : {}", wrongSentences);

            model.addAttribute("totalCount", totalCount);
            model.addAttribute("correctCount", correctCount);
            model.addAttribute("correctPercentage", totalCount > 0 ? (correctCount * 100 / totalCount) : 0);
            model.addAttribute("correctSentences", correctSentences);
            model.addAttribute("wrongSentences", wrongSentences);
            model.addAttribute("analysis", analysis);
            model.addAttribute("noData", false);
            model.addAttribute("error", null); // Explicitly set to avoid null checks

        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error analyzing progress", e);
            model.addAttribute("error", "Could not analyze your progress. Please try again later.");
            model.addAttribute("noData", false); // Explicitly set to avoid null checks
        }
        return "progress-analysis";
    }
}
