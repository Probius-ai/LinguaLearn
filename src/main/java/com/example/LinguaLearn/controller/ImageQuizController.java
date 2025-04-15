package com.example.LinguaLearn.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.WordService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ImageQuizController {

    private static final Logger logger = LoggerFactory.getLogger(ImageQuizController.class);
    private static final String UNSPLASH_API_KEY = "lim93_dY_RdGEV_J7rHtAwbASLPE6N4PgC8vKRUkxNs";
    
    @Autowired
    private WordService wordService;
    
    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/quiz/image")
    public String showQuizPage() {
        return "image-quiz";
    }
    
    @GetMapping("/api/quiz/image")
    @ResponseBody
    public ResponseEntity<?> getImageQuiz(
            @RequestParam String level, 
            @RequestParam String language,
            HttpSession session) {
        try {
            // Get the current user from session
            User user = (User) session.getAttribute("user");
            if (user == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
                }
            }
            
            List<String> wordList = wordService.getWordsByLevelAndLanguage(level, language);
            
            if (wordList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "단어가 없습니다."));
            }

            Random rand = new Random();
            String answer = wordList.get(rand.nextInt(wordList.size()));

            Set<String> choices = new HashSet<>();
            choices.add(answer);
            while (choices.size() < 4 && choices.size() < wordList.size()) {
                choices.add(wordList.get(rand.nextInt(wordList.size())));
            }

            // Use English translation for image search
            String searchWord = translateForImageSearch(answer, language);
            String imageUrl = fetchImageFromUnsplash(searchWord);

            Map<String, Object> quiz = new HashMap<>();
            quiz.put("answer", answer);
            quiz.put("choices", shuffleList(new ArrayList<>(choices)));
            quiz.put("imageUrl", imageUrl);

            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            logger.error("Error generating image quiz", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }
    
    @PostMapping("/api/quiz/score")
    @ResponseBody
    public ResponseEntity<?> updateScore(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        
        try {
            // In a real implementation, you would update the user's score in the database
            // For now, let's just return success
            return ResponseEntity.ok(Map.of("success", true, "message", "점수가 업데이트되었습니다."));
        } catch (Exception e) {
            logger.error("Error updating user score", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false, 
                "message", "점수 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    private String translateForImageSearch(String word, String language) {
        // In a real implementation, you would use a translation service
        // For simple demo purposes, returning the original word
        return word;
    }

    private String fetchImageFromUnsplash(String searchWord) {
        try {
            String url = "https://api.unsplash.com/search/photos?query=" + searchWord +
                    "&per_page=1&client_id=" + UNSPLASH_API_KEY;

            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("results")) {
                List results = (List) response.get("results");
                if (!results.isEmpty()) {
                    Map firstResult = (Map) results.get(0);
                    Map urls = (Map) firstResult.get("urls");
                    return urls.get("regular").toString();
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching image from Unsplash", e);
        }

        return "https://via.placeholder.com/400x300?text=No+Image";
    }

    private List<String> shuffleList(List<String> list) {
        Collections.shuffle(list);
        return list;
    }
}