package com.example.LinguaLearn.model;

import com.google.cloud.firestore.annotation.DocumentId;

public class Word {
    @DocumentId
    private String id;            // 단어 ID (Firestore 문서 ID)
    private String text;          // 단어 텍스트
    private String translation;   // 번역
    private String pronunciation; // 발음 기호
    private String exampleSentence; // 예문

    // Firestore 기본 생성자
    public Word() {}

    // 필수 필드만 있는 생성자
    public Word(String text, String translation) {
        this.text = text;
        this.translation = translation;
    }

    // 모든 필드를 포함한 생성자
    public Word(String id, String text, String translation, 
                String pronunciation, String exampleSentence) {
        this.id = id;
        this.text = text;
        this.translation = translation;
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
    public String getPronunciation() { return pronunciation; }
    public void setPronunciation(String pronunciation) { this.pronunciation = pronunciation; }
    public String getExampleSentence() { return exampleSentence; }
    public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }

    @Override
    public String toString() {
        return "Word{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", translation='" + translation + '\'' +
                ", pronunciation='" + pronunciation + '\'' +
                ", exampleSentence='" + exampleSentence + '\'' +
                '}';
    }
}