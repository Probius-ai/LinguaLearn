package com.example.LinguaLearn.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Repository
public class FirestoreRepository {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreRepository.class);
    private final Firestore firestore;

    @Autowired
    public FirestoreRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Get a document from Firestore
     * @param collection The collection name
     * @param documentId The document ID
     * @return The document snapshot
     */
    public DocumentSnapshot getDocument(String collection, String documentId) throws InterruptedException, ExecutionException {
        DocumentReference docRef = firestore.collection(collection).document(documentId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        return future.get();
    }

    /**
     * Get a user's progress document
     * @param uid The user ID
     * @return The progress document snapshot
     */
    public DocumentSnapshot getProgress(String uid) throws InterruptedException, ExecutionException {
        return getDocument("progress", uid);
    }

    /**
     * Update a document in Firestore
     * @param collection The collection name
     * @param documentId The document ID
     * @param data The data to update
     * @return The write result
     */
    public WriteResult updateDocument(String collection, String documentId, Map<String, Object> data)
            throws InterruptedException, ExecutionException {
        DocumentReference docRef = firestore.collection(collection).document(documentId);
        ApiFuture<WriteResult> future = docRef.set(data);
        return future.get();
    }
}