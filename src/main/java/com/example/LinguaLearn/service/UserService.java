package com.example.LinguaLearn.service;

import com.example.LinguaLearn.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

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

    public User createOrUpdateUser(FirebaseToken decodedToken) throws ExecutionException, InterruptedException {
        String uid = decodedToken.getUid();
        User existingUser = getUser(uid);

        if (existingUser == null) {
            logger.info("Creating new user profile for UID: {}", uid);
            User newUser = new User(uid, decodedToken.getEmail(), decodedToken.getName());
            // You might want to set other default fields here
            ApiFuture<WriteResult> future = firestore.collection(USERS_COLLECTION).document(uid).set(newUser);
            logger.info("User profile created at: {}", future.get().getUpdateTime());
            return newUser;
        } else {
            // Optionally update existing user data if needed (e.g., display name changes)
             logger.debug("User profile already exists for UID: {}", uid);
            // Example update (uncomment if needed):
            // if (!Objects.equals(existingUser.getDisplayName(), decodedToken.getName()) ||
            //     !Objects.equals(existingUser.getEmail(), decodedToken.getEmail())) {
            //     existingUser.setDisplayName(decodedToken.getName());
            //     existingUser.setEmail(decodedToken.getEmail());
            //     ApiFuture<WriteResult> future = firestore.collection(USERS_COLLECTION).document(uid).set(existingUser, SetOptions.merge());
            //     logger.info("User profile updated at: {}", future.get().getUpdateTime());
            // }
            return existingUser;
        }
    }
}
