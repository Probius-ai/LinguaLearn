package com.example.LinguaLearn.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.LinguaLearn.model.Word;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

@Service
public class WordService {

    private static final Logger logger = LoggerFactory.getLogger(WordService.class);
    
    @Autowired
    private Firestore firestore;
    
    // 지원되는 언어와 레벨
    private static final String[] SUPPORTED_LANGUAGES = {"english", "japanese", "chinese"};
    private static final String[] SUPPORTED_LEVELS = {"beginner", "intermediate", "advanced"};
    
    /**
     * 모든 단어 가져오기 (관리자 UI용)
     */
    public List<WordDto> getAllWords() throws ExecutionException, InterruptedException {
        List<WordDto> allWords = new ArrayList<>();
        
        for (String language : SUPPORTED_LANGUAGES) {
            for (String level : SUPPORTED_LEVELS) {
                List<WordDto> words = getWordsByLanguageAndLevel(language, level);
                allWords.addAll(words);
            }
        }
        
        return allWords;
    }
    
    /**
     * 특정 언어의 모든 단어 가져오기
     */
    public List<WordDto> getWordsByLanguage(String language) throws ExecutionException, InterruptedException {
        List<WordDto> languageWords = new ArrayList<>();
        
        for (String level : SUPPORTED_LEVELS) {
            List<WordDto> words = getWordsByLanguageAndLevel(language, level);
            languageWords.addAll(words);
        }
        
        return languageWords;
    }
    
    /**
     * 특정 레벨의 모든 단어 가져오기
     */
    public List<WordDto> getWordsByLevel(String level) throws ExecutionException, InterruptedException {
        List<WordDto> levelWords = new ArrayList<>();
        
        for (String language : SUPPORTED_LANGUAGES) {
            List<WordDto> words = getWordsByLanguageAndLevel(language, level);
            levelWords.addAll(words);
        }
        
        return levelWords;
    }
    
    /**
     * 특정 언어와 레벨의 단어 가져오기
     */
    public List<WordDto> getWordsByLanguageAndLevel(String language, String level) 
            throws ExecutionException, InterruptedException {
        
        // 언어와 레벨 검증
        if (!isValidLanguage(language) || !isValidLevel(level)) {
            logger.warn("잘못된 언어 또는 레벨: language={}, level={}", language, level);
            return new ArrayList<>();
        }
        
        // Firestore 경로 구성
        CollectionReference wordsCollection = firestore
            .collection("languages")
            .document(language)
            .collection("levels")
            .document(level)
            .collection("words");
        
        // 데이터 조회
        ApiFuture<QuerySnapshot> future = wordsCollection.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        // 결과 변환
        List<WordDto> words = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Word word = document.toObject(Word.class);
            
            // DTO로 변환하고 언어와 레벨 정보 추가
            WordDto dto = new WordDto(
                document.getId(),
                word.getText(),
                word.getTranslation(),
                language,
                level,
                word.getPronunciation(),
                word.getExampleSentence()
            );
            
            words.add(dto);
        }
        
        return words;
    }
    
    /**
     * 단어 하나 가져오기
     */
    public WordDto getWordById(String language, String level, String id) 
            throws ExecutionException, InterruptedException {
        
        // 언어와 레벨 검증
        if (!isValidLanguage(language) || !isValidLevel(level)) {
            logger.warn("잘못된 언어 또는 레벨: language={}, level={}", language, level);
            return null;
        }
        
        // Firestore 경로 구성
        DocumentReference docRef = firestore
            .collection("languages")
            .document(language)
            .collection("levels")
            .document(level)
            .collection("words")
            .document(id);
        
        // 데이터 조회
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        
        if (document.exists()) {
            Word word = document.toObject(Word.class);
            
            // DTO로 변환하고 언어와 레벨 정보 추가
            return new WordDto(
                document.getId(),
                word.getText(),
                word.getTranslation(),
                language,
                level,
                word.getPronunciation(),
                word.getExampleSentence()
            );
        } else {
            return null;
        }
    }
    
    /**
     * 단어 추가하기
     */
    public WordDto createWord(WordDto wordDto) throws ExecutionException, InterruptedException {
        // 언어와 레벨 검증
        if (!isValidLanguage(wordDto.getLanguage()) || !isValidLevel(wordDto.getLevel())) {
            logger.warn("잘못된 언어 또는 레벨: language={}, level={}", 
                      wordDto.getLanguage(), wordDto.getLevel());
            return null;
        }
        
        // DTO를 Entity로 변환
        Word word = new Word();
        word.setText(wordDto.getText());
        word.setTranslation(wordDto.getTranslation());
        word.setPronunciation(wordDto.getPronunciation());
        word.setExampleSentence(wordDto.getExampleSentence());
        
        // ID가 없으면 생성
        String wordId = wordDto.getId();
        if (wordId == null || wordId.isEmpty()) {
            wordId = UUID.randomUUID().toString();
        }
        
        // Firestore 경로 구성
        DocumentReference docRef = firestore
            .collection("languages")
            .document(wordDto.getLanguage())
            .collection("levels")
            .document(wordDto.getLevel())
            .collection("words")
            .document(wordId);
        
        // 데이터 저장
        ApiFuture<WriteResult> future = docRef.set(word);
        future.get(); // 작업 완료 대기
        
        logger.info("단어 추가 완료: {}({})", word.getText(), wordId);
        
        // 저장 완료 후 DTO 반환 (ID 포함)
        wordDto.setId(wordId);
        return wordDto;
    }
    
    /**
     * 단어 업데이트
     */
    public WordDto updateWord(String language, String level, String id, WordDto wordDto) 
            throws ExecutionException, InterruptedException {
        
        // 언어와 레벨 검증
        if (!isValidLanguage(language) || !isValidLevel(level)) {
            logger.warn("잘못된 언어 또는 레벨: language={}, level={}", language, level);
            return null;
        }
        
        // 문서 존재 확인
        DocumentReference docRef = firestore
            .collection("languages")
            .document(language)
            .collection("levels")
            .document(level)
            .collection("words")
            .document(id);
        
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        
        if (!document.exists()) {
            logger.error("ID {}인 단어를 찾을 수 없습니다.", id);
            return null;
        }
        
        // 다른 언어나 레벨로 이동하는 경우
        if (!language.equals(wordDto.getLanguage()) || !level.equals(wordDto.getLevel())) {
            // 트랜잭션 사용하여 원자적 업데이트 수행
            firestore.runTransaction(transaction -> {
                // 기존 문서 삭제
                transaction.delete(docRef);
                
                // 새 위치에 추가
                DocumentReference newDocRef = firestore
                    .collection("languages")
                    .document(wordDto.getLanguage())
                    .collection("levels")
                    .document(wordDto.getLevel())
                    .collection("words")
                    .document(id);
                    
                Word word = new Word();
                word.setText(wordDto.getText());
                word.setTranslation(wordDto.getTranslation());
                word.setPronunciation(wordDto.getPronunciation());
                word.setExampleSentence(wordDto.getExampleSentence());
                
                transaction.set(newDocRef, word);
                return null;
            }).get();
            
            logger.info("단어 이동 완료: {}({})", wordDto.getText(), id);
            return wordDto;
        }
        
        // 동일한 위치에서 업데이트하는 경우
        Word word = new Word();
        word.setText(wordDto.getText());
        word.setTranslation(wordDto.getTranslation());
        word.setPronunciation(wordDto.getPronunciation());
        word.setExampleSentence(wordDto.getExampleSentence());
        
        // 데이터 업데이트
        ApiFuture<WriteResult> updateResult = docRef.set(word);
        updateResult.get();
        
        logger.info("단어 업데이트 완료: {}({})", word.getText(), id);
        return wordDto;
    }    
    
    /**
     * 단어 삭제
     */
    public boolean deleteWord(String language, String level, String id) 
            throws ExecutionException, InterruptedException {
        
        // 언어와 레벨 검증
        if (!isValidLanguage(language) || !isValidLevel(level)) {
            logger.warn("잘못된 언어 또는 레벨: language={}, level={}", language, level);
            return false;
        }
        
        // 문서 경로 구성
        DocumentReference docRef = firestore
            .collection("languages")
            .document(language)
            .collection("levels")
            .document(level)
            .collection("words")
            .document(id);
        
        // 문서 존재 확인
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        
        if (!document.exists()) {
            logger.error("ID {}인 단어를 찾을 수 없습니다.", id);
            return false;
        }
        
        // 문서 삭제
        ApiFuture<WriteResult> deleteResult = docRef.delete();
        deleteResult.get();
        
        logger.info("단어 삭제 완료: {}", id);
        return true;
    }
    
    /**
     * 퀴즈용 단어 목록 가져오기 (텍스트만)
     * ImageQuizController에서 사용됨
     */
    public List<String> getWordsForQuiz(String language, String level) 
            throws ExecutionException, InterruptedException {
        
        List<WordDto> wordDtos = getWordsByLanguageAndLevel(language, level);
        return wordDtos.stream()
                .map(WordDto::getText)
                .collect(Collectors.toList());
    }
    
    // 유효한 언어인지 확인
    private boolean isValidLanguage(String language) {
        if (language == null) return false;
        
        for (String supportedLang : SUPPORTED_LANGUAGES) {
            if (supportedLang.equals(language)) {
                return true;
            }
        }
        return false;
    }
    
    // 유효한 레벨인지 확인
    private boolean isValidLevel(String level) {
        if (level == null) return false;
        
        for (String supportedLevel : SUPPORTED_LEVELS) {
            if (supportedLevel.equals(level)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 단어 DTO 클래스 (추가/수정 요청 및 응답에 사용)
     */
    public static class WordDto {
        private String id;
        private String text;
        private String translation;
        private String language;
        private String level;
        private String pronunciation;
        private String exampleSentence;
        
        public WordDto() {}
        
        public WordDto(String id, String text, String translation, String language, String level,
                      String pronunciation, String exampleSentence) {
            this.id = id;
            this.text = text;
            this.translation = translation;
            this.language = language;
            this.level = level;
            this.pronunciation = pronunciation;
            this.exampleSentence = exampleSentence;
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getTranslation() { return translation; }
        public void setTranslation(String translation) { this.translation = translation; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getPronunciation() { return pronunciation; }
        public void setPronunciation(String pronunciation) { this.pronunciation = pronunciation; }
        public String getExampleSentence() { return exampleSentence; }
        public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }
    }
}