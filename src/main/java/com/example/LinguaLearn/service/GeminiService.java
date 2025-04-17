package com.example.LinguaLearn.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

@Slf4j
@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private final RestTemplate restTemplate;
    private final Firestore firestore;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Autowired
    public GeminiService(RestTemplate restTemplate, Firestore firestore) {
        this.restTemplate = restTemplate;
        this.firestore = firestore;
    }

    public String getContents(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        
        parts.put("text", prompt);
        content.put("parts", List.of(parts));
        requestBody.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            // apiUrl already contains the key in the URL
            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    if (candidate.containsKey("content")) {
                        Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                        if (contentMap.containsKey("parts")) {
                            List<Map<String, Object>> parts_response = (List<Map<String, Object>>) contentMap.get("parts");
                            if (!parts_response.isEmpty() && parts_response.get(0).containsKey("text")) {
                                return (String) parts_response.get(0).get("text");
                            }
                        }
                    }
                }
            }
            logger.error("Unexpected response structure from Gemini API: {}", response);
            return "Unable to get a proper response from AI service.";
        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            return "Error communicating with AI service: " + e.getMessage();
        }
    }

    public Map<String, String> getTodayWord(String language, String level) {
        // Get today's date in YYYY-MM-DD format
        String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        
        try {
            // Check if today's word is already in Firestore
            DocumentReference docRef = firestore
                .collection("daily_words")
                .document(todayDate + "_" + language + "_" + level);
                
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            // If today's word exists, return it
            if (document.exists()) {
                Map<String, Object> data = document.getData();
                Map<String, String> wordData = new HashMap<>();
                wordData.put("word", (String) data.get("word"));
                wordData.put("translation", (String) data.get("translation"));
                wordData.put("pronunciation", (String) data.get("pronunciation"));
                wordData.put("exampleSentence", (String) data.get("exampleSentence"));
                wordData.put("partOfSpeech", (String) data.get("partOfSpeech"));
                
                logger.info("Retrieved today's word from Firestore: {}", wordData.get("word"));
                return wordData;
            }
            
            // If not, generate a new word with Gemini API
            String prompt = String.format(
                "Give me a %s level %s word that is useful for language learners. " +
                "Respond in JSON format with these fields: word, translation (in Korean), " +
                "pronunciation, partOfSpeech, exampleSentence. " +
                "Just provide the JSON with no other text.",
                level, language
            );
            
            String response = getContents(prompt);
            logger.info("Gemini API response for today's word: {}", response);
            
            // Parse the response and extract word details
            // This is a simplified parsing. In production, use a JSON parser.
            Map<String, String> wordData = parseJsonResponse(response);
            
            // Store in Firestore
            docRef.set(wordData);
            logger.info("Stored new word in Firestore: {}", wordData.get("word"));
            
            return wordData;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error getting today's word", e);
            throw new RuntimeException("Error retrieving today's word", e);
        }
    }

    public String translateText(String text, String fromLanguage, String toLanguage) {
        String prompt = String.format(
            "Translate the following text from %s to %s: \"%s\"",
            fromLanguage, toLanguage, text
        );
        
        return getContents(prompt);
    }

    public String generateSentenceForTest(String language, String level) {
        String prompt = String.format(
            "Generate one simple %s sentence for %s level language learners. " +
            "The sentence should be about daily life and suitable for a translation exercise. " +
            "Provide only the sentence with no additional explanation.",
            language, level
        );
        
        return getContents(prompt);
    }

    public String analyzeLearningProgress(List<String> correctSentences, List<String> wrongSentences) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Based on a language learner's performance, provide a brief analysis and improvement tips. ");
        
        promptBuilder.append("Correctly translated sentences: ");
        promptBuilder.append(String.join(", ", correctSentences));
        promptBuilder.append(". ");
        
        promptBuilder.append("Incorrectly translated sentences: ");
        promptBuilder.append(String.join(", ", wrongSentences));
        promptBuilder.append(". ");
        
        promptBuilder.append("Keep the analysis concise and focused on actionable advice.");
        
        return getContents(promptBuilder.toString());
    }
    
    // Helper method to parse JSON response from Gemini API
    private Map<String, String> parseJsonResponse(String jsonString) {
        Map<String, String> result = new HashMap<>();
        
        // Very simple JSON parsing for demonstration
        // In production, use a proper JSON parser
        jsonString = jsonString.replaceAll("[{}\"]", "");
        String[] pairs = jsonString.split(",");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                result.put(key, value);
            }
        }
        
        return result;
    }
    public String getResult(String wrongSentence, String userTranslation) {
        String prompt = wrongSentence + " 를 번역한 결과가 " + userTranslation +
                "이 맞는지 판단해줘(정답, 오답) *은 출력하지마, 이유를 알려줘(**은 출력하지마)";
        log.info("Retry evaluation prompt: {}", prompt);
        return getContents(prompt);
    }
}
