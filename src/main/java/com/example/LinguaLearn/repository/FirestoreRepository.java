package com.example.LinguaLearn.repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

@Repository
public class FirestoreRepository {
    private final Firestore firestore;

    public FirestoreRepository() {
        this.firestore = FirestoreClient.getFirestore();
    }

    public DocumentSnapshot getProgress(String uid) throws InterruptedException, ExecutionException {
        DocumentReference progressDoc = firestore.collection("progress").document(uid);
        return progressDoc.get().get();
    }
}
