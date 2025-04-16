package com.example.LinguaLearn.model;

import com.google.cloud.firestore.annotation.DocumentId;

public class User {
    @DocumentId // Marks this field as the document ID
    private String uid;
    private String email;
    private String displayName;
    private String primaryLanguage; // 추가: 사용자 주 학습 언어
    private Integer dailyGoal; // 추가: 일일 학습 목표 (분)
    private Boolean pushNotification; // 추가: 푸시 알림 설정

    // Firestore requires a no-arg constructor
    public User() {}

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.primaryLanguage = "english"; // 기본값 설정
        this.dailyGoal = 15; // 기본값 설정 (15분)
        this.pushNotification = true; // 기본값 설정
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(String primaryLanguage) { this.primaryLanguage = primaryLanguage; }

    public Integer getDailyGoal() { return dailyGoal; }
    public void setDailyGoal(Integer dailyGoal) { this.dailyGoal = dailyGoal; }

    public Boolean getPushNotification() { return pushNotification; }
    public void setPushNotification(Boolean pushNotification) { this.pushNotification = pushNotification; }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", primaryLanguage='" + primaryLanguage + '\'' +
                ", dailyGoal=" + dailyGoal +
                ", pushNotification=" + pushNotification +
                '}';
    }
}