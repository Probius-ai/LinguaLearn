package com.example.LinguaLearn.service;

import com.example.LinguaLearn.repository.FirestoreRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class FirestoreService {
    private final FirestoreRepository firestoreRepository;

    public FirestoreService(FirestoreRepository firestoreRepository) {
        this.firestoreRepository = firestoreRepository;
    }

    public List<String> getWrongProblem(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestoreRepository.getProgress(uid);

        // Firestore에 저장된 오답 목록 조회 (없는 경우 빈 리스트)
        List<String> wrongSentences = snapshot.contains("wrong_sentence")
                ? (List<String>) snapshot.get("wrong_sentence") : new ArrayList<>();
        List<String> englishSentences = new ArrayList<>();
        for (Object obj : wrongSentences) {
            if (obj instanceof HashMap) {
                HashMap<?, ?> map = (HashMap<?, ?>) obj;
                Object sentenceObj = map.get("sentence");
                if (sentenceObj instanceof String) {
                    englishSentences.add((String) sentenceObj);
                }
            }
        }
        return englishSentences;
    }
}
