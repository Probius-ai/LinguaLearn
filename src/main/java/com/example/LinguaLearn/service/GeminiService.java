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

import com.fasterxml.jackson.core.JsonProcessingException; // Added import
import com.fasterxml.jackson.core.type.TypeReference; // Added import
import com.fasterxml.jackson.databind.ObjectMapper; // Added import
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private final RestTemplate restTemplate;
    private final Firestore firestore;
    private final ObjectMapper objectMapper; // Added ObjectMapper

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Autowired
    public GeminiService(RestTemplate restTemplate, Firestore firestore, ObjectMapper objectMapper) { // Inject ObjectMapper
        this.restTemplate = restTemplate;
        this.firestore = firestore;
        this.objectMapper = objectMapper; // Assign ObjectMapper
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
        String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String docId = todayDate + "_" + language + "_" + level;
        DocumentReference docRef = firestore.collection("daily_words").document(docId);

        String prompt = String.format(
            "Give me a %s level %s word that is useful for language learners. " +
            "Respond ONLY with a valid JSON object containing these fields: " +
            "\"word\", \"translation\" (in Korean), \"pronunciation\", \"partOfSpeech\", \"exampleSentence\". " +
            "Do not include any text before or after the JSON object, and do not use markdown formatting like ```json.",
            level, language
        );
        
        try {
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (document.exists()) {
                Map<String, Object> data = document.getData();
                // Convert Firestore data (Map<String, Object>) to Map<String, String>
                Map<String, String> wordData = new HashMap<>();
                if (data != null) {
                    data.forEach((key, value) -> wordData.put(key, value != null ? value.toString() : null));
                }
                logger.info("Retrieved today's word from Firestore: {}", wordData.get("word"));
                return wordData;
            }

            String responseJson = getContents(prompt);
            // Clean potential markdown fences if the LLM still adds them
            responseJson = responseJson.trim().replaceFirst("^```json", "").replaceFirst("```$", "").trim();
            logger.info("Gemini API response for today's word (raw): {}", responseJson);

            // Use ObjectMapper to parse the JSON response
            Map<String, String> wordData = objectMapper.readValue(responseJson, new TypeReference<Map<String, String>>() {});

            // Store in Firestore
            docRef.set(wordData); // Storing Map<String, String> directly
            logger.info("Stored new word in Firestore: {}", wordData.get("word"));

            return wordData;

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error interacting with Firestore for docId: {}", docId, e);
            throw new RuntimeException("Error retrieving today's word from Firestore", e);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON response from Gemini API for docId: {}. Response: {}", docId, getContents(prompt), e); // Log the raw response on error
            throw new RuntimeException("Error parsing today's word data from AI service", e);
        } catch (Exception e) { // Catch other potential exceptions
            logger.error("Unexpected error getting today's word for docId: {}", docId, e);
            throw new RuntimeException("Unexpected error retrieving today's word", e);
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
                    "Please vary the topic and vocabulary each time to avoid repetition. " +
                    "Use different contexts (e.g., hobbies, work, food, travel). " +
            "Provide only the sentence with no additional explanation.",
            language, level
        );
        
        return getContents(prompt);
    }

    public String analyzeLearningProgress(List<String> correctSentences, List<String> wrongSentences) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Based on a language learner's performance, provide a brief analysis and improvement tips in Korean. ");
        promptBuilder.append("Write your response only in Korean.");

        promptBuilder.append("Correctly translated sentences: ");
        promptBuilder.append(String.join(", ", correctSentences));
        promptBuilder.append(". ");
        
        promptBuilder.append("Incorrectly translated sentences: ");
        promptBuilder.append(String.join(", ", wrongSentences));
        promptBuilder.append(". ");
        
        promptBuilder.append("Keep the analysis concise and focused on actionable advice");
        
        return getContents(promptBuilder.toString());
    }
}
