package com.example.LinguaLearn.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;

@Service
public class WordService {

    private static final Logger logger = LoggerFactory.getLogger(WordService.class);
    
    @Autowired
    private Firestore firestore;
    
    // Collection names for different languages
    private static final String ENGLISH_COLLECTION = "words";
    private static final String JAPANESE_COLLECTION = "wordsJP";
    private static final String CHINESE_COLLECTION = "wordsCN";
    
    /**
     * Get words for a specific level and language
     * 
     * @param level Level (beginner, intermediate, advanced)
     * @param language Language (english, japanese, chinese)
     * @return List of words
     */
    public List<String> getWordsByLevelAndLanguage(String level, String language) 
            throws ExecutionException, InterruptedException {
        
        String collection = getCollectionForLanguage(language);
        String fieldName = getFieldNameForLanguage(language);
        
        DocumentSnapshot doc = firestore.collection(collection).document(level).get().get();
        logger.debug("Fetching words for level: {}, language: {}", level, language);
        
        if (doc.exists()) {
            Object raw = doc.get(fieldName);
            if (raw instanceof List<?>) {
                return ((List<?>) raw).stream().map(Object::toString).collect(Collectors.toList());
            }
        }
        
        logger.warn("No words found for level: {}, language: {}", level, language);
        return new ArrayList<>();
    }
    
    /**
     * Add a word to a language collection
     */
    public void addWord(String level, String word, String language) 
            throws ExecutionException, InterruptedException {
        
        String collection = getCollectionForLanguage(language);
        String fieldName = getFieldNameForLanguage(language);
        
        firestore.collection(collection).document(level)
                .update(fieldName, FieldValue.arrayUnion(word))
                .get();
        
        logger.info("Added word '{}' to level: {}, language: {}", word, level, language);
    }
    
    /**
     * Remove a word from a language collection
     */
    public void removeWord(String level, String word, String language) 
            throws ExecutionException, InterruptedException {
        
        String collection = getCollectionForLanguage(language);
        String fieldName = getFieldNameForLanguage(language);
        
        firestore.collection(collection).document(level)
                .update(fieldName, FieldValue.arrayRemove(word))
                .get();
        
        logger.info("Removed word '{}' from level: {}, language: {}", word, level, language);
    }
    
    // Helper methods to determine collection and field names based on language
    
    private String getCollectionForLanguage(String language) {
        switch (language.toLowerCase()) {
            case "japanese":
                return JAPANESE_COLLECTION;
            case "chinese":
                return CHINESE_COLLECTION;
            default:
                return ENGLISH_COLLECTION;
        }
    }
    
    private String getFieldNameForLanguage(String language) {
        switch (language.toLowerCase()) {
            case "japanese":
            case "chinese":
                return "word";
            default:
                return "words";
        }
    }
}