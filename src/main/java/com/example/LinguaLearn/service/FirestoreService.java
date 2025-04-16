package com.example.LinguaLearn.service;

import com.example.LinguaLearn.repository.FirestoreRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreService.class);
    private final FirestoreRepository firestoreRepository;

    @Autowired
    public FirestoreService(FirestoreRepository firestoreRepository) {
        this.firestoreRepository = firestoreRepository;
    }

    /**
     * Get a user's wrong sentences for review
     *
     * @param uid The user ID
     * @return List of sentences the user got wrong
     */
    public List<String> getWrongProblem(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestoreRepository.getProgress(uid);
        List<String> englishSentences = new ArrayList<>();

        if (!snapshot.exists()) {
            logger.info("No progress document found for user: {}", uid);
            return englishSentences;
        }

        // Firestore에 저장된 오답 목록 조회
        List<Map<String, String>> wrongSentences = snapshot.contains("wrong_sentence")
                ? (List<Map<String, String>>) snapshot.get("wrong_sentence")
                : new ArrayList<>();

        if (wrongSentences == null || wrongSentences.isEmpty()) {
            logger.info("No wrong sentences found for user: {}", uid);
            return englishSentences;
        }

        // Extract original sentences from the wrong_sentence field
        for (Map<String, String> entry : wrongSentences) {
            if (entry.containsKey("sentence")) {
                englishSentences.add(entry.get("sentence"));
            }
        }

        logger.info("Retrieved {} wrong sentences for user: {}", englishSentences.size(), uid);
        return englishSentences;
    }

    /**
     * Get a user's progress statistics
     *
     * @param uid The user ID
     * @return Map containing total count, correct count, and wrong count
     */
    public Map<String, Object> getUserProgressStats(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestoreRepository.getProgress(uid);
        Map<String, Object> stats = new HashMap<>();

        if (!snapshot.exists()) {
            stats.put("total_cnt", 0L);
            stats.put("correct_cnt", 0L);
            stats.put("wrong_cnt", 0L);
            return stats;
        }

        Long totalCount = snapshot.getLong("total_cnt");
        Long correctCount = snapshot.getLong("correct_cnt");
        Long wrongCount = snapshot.getLong("wrong_cnt");

        stats.put("total_cnt", totalCount != null ? totalCount : 0L);
        stats.put("correct_cnt", correctCount != null ? correctCount : 0L);
        stats.put("wrong_cnt", wrongCount != null ? wrongCount : 0L);

        return stats;
    }

    /**
     * Get correct and wrong sentences for a user
     */
    public Map<String, List<Map<String, String>>> getUserSentences(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestoreRepository.getProgress(uid);
        Map<String, List<Map<String, String>>> sentences = new HashMap<>();

        if (!snapshot.exists()) {
            sentences.put("correct_sentence", new ArrayList<>());
            sentences.put("wrong_sentence", new ArrayList<>());
            return sentences;
        }

        List<Map<String, String>> correctSentences = snapshot.contains("correct_sentence")
                ? (List<Map<String, String>>) snapshot.get("correct_sentence")
                : new ArrayList<>();

        List<Map<String, String>> wrongSentences = snapshot.contains("wrong_sentence")
                ? (List<Map<String, String>>) snapshot.get("wrong_sentence")
                : new ArrayList<>();

        sentences.put("correct_sentence", correctSentences != null ? correctSentences : new ArrayList<>());
        sentences.put("wrong_sentence", wrongSentences != null ? wrongSentences : new ArrayList<>());

        return sentences;
    }
}