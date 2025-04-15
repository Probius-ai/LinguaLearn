package com.example.LinguaLearn.service;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.LinguaLearn.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String USERS_COLLECTION = "users";

    @Autowired
    private Firestore firestore;

    public User getUser(String uid) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(uid);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(User.class);
        } else {
            return null;
        }
    }

    /**
     * FirebaseToken 대신 개별 필드를 받아 사용자 생성/업데이트
     */
    public User createOrUpdateUser(String uid, String email, String displayName) throws ExecutionException, InterruptedException {
        User existingUser = getUser(uid);

        if (existingUser == null) {
            logger.info("Creating new user profile for UID: {}", uid);
            User newUser = new User(uid, email, displayName);
            // You might want to set other default fields here
            ApiFuture<WriteResult> future = firestore.collection(USERS_COLLECTION).document(uid).set(newUser);
            logger.info("User profile created at: {}", future.get().getUpdateTime());
            return newUser;
        } else {
            // Optionally update existing user data if needed (e.g., display name changes)
            logger.debug("User profile already exists for UID: {}", uid);
            // Example update (uncomment if needed):
            // if (!Objects.equals(existingUser.getDisplayName(), displayName) ||
            //     !Objects.equals(existingUser.getEmail(), email)) {
            //     existingUser.setDisplayName(displayName);
            //     existingUser.setEmail(email);
            //     ApiFuture<WriteResult> future = firestore.collection(USERS_COLLECTION).document(uid).set(existingUser, SetOptions.merge());
            //     logger.info("User profile updated at: {}", future.get().getUpdateTime());
            // }
            return existingUser;
        }
    }
}