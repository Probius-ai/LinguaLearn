package com.example.LinguaLearn.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
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
public class RankingService {

    private static final Logger logger = LoggerFactory.getLogger(RankingService.class);

    @Autowired
    private Firestore firestore;

    /**
     * Get top users by total score
     *
     * @param limit The number of top users to retrieve
     * @return List of user ranking data
     */
    public List<Map<String, Object>> getTopRankings(int limit) throws ExecutionException, InterruptedException {
        logger.info("Fetching top {} users for ranking", limit);

        // Get data from progress collection, ordered by total score
        ApiFuture<QuerySnapshot> future = firestore.collection("progress")
                .orderBy("total_score", Query.Direction.DESCENDING)
                .limit(limit)
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Map<String, Object>> rankings = new ArrayList<>();

        for (QueryDocumentSnapshot document : documents) {
            String uid = document.getId();
            Long score = document.getLong("total_score");

            if (score == null) {
                score = 0L;
            }

            // Get user details from users collection
            DocumentSnapshot userDoc = firestore.collection("users").document(uid).get().get();

            Map<String, Object> rankData = new HashMap<>();
            rankData.put("uid", uid);
            rankData.put("score", score);

            // Add user details if available
            if (userDoc.exists()) {
                rankData.put("displayName", userDoc.getString("displayName"));
                rankData.put("email", userDoc.getString("email"));

                // Mask email for privacy (show only first 3 characters + domain)
                String email = userDoc.getString("email");
                if (email != null && email.contains("@")) {
                    String[] parts = email.split("@");
                    if (parts[0].length() > 3) {
                        String maskedEmail = parts[0].substring(0, 3) + "***@" + parts[1];
                        rankData.put("maskedEmail", maskedEmail);
                    } else {
                        rankData.put("maskedEmail", email);
                    }
                }
            } else {
                rankData.put("displayName", "Unknown User");
                rankData.put("email", "unknown@example.com");
                rankData.put("maskedEmail", "unknown@example.com");
            }

            rankings.add(rankData);
        }

        return rankings;
    }

    /**
     * Get a specific user's rank
     *
     * @param uid The user ID
     * @return User's rank data
     */
    public Map<String, Object> getUserRank(String uid) throws ExecutionException, InterruptedException {
        logger.info("Fetching rank for user: {}", uid);

        // Get the user's progress
        DocumentSnapshot userProgress = firestore.collection("progress").document(uid).get().get();
        Map<String, Object> rankData = new HashMap<>();

        if (!userProgress.exists()) {
            rankData.put("score", 0L);
            rankData.put("rank", "N/A");
            return rankData;
        }

        Long userScore = userProgress.getLong("total_score");
        if (userScore == null) {
            userScore = 0L;
        }

        // Count how many users have higher scores
        ApiFuture<QuerySnapshot> future = firestore.collection("progress")
                .whereGreaterThan("total_score", userScore)
                .get();

        int higherScoreCount = future.get().size();
        int rank = higherScoreCount + 1;

        rankData.put("score", userScore);
        rankData.put("rank", rank);

        return rankData;
    }

    /**
     * Update user's total score (called after completing quizzes)
     *
     * @param uid The user ID
     * @param additionalPoints Points to add
     */
    public void updateUserScore(String uid, long additionalPoints) throws ExecutionException, InterruptedException {
        logger.info("Updating score for user: {} with {} points", uid, additionalPoints);

        DocumentSnapshot userProgress = firestore.collection("progress").document(uid).get().get();
        Map<String, Object> updates = new HashMap<>();

        if (userProgress.exists()) {
            Long currentScore = userProgress.getLong("total_score");
            if (currentScore == null) {
                currentScore = 0L;
            }

            updates.put("total_score", currentScore + additionalPoints);
        } else {
            updates.put("total_score", additionalPoints);
        }

        firestore.collection("progress").document(uid).set(updates, com.google.cloud.firestore.SetOptions.merge());
    }
}