package com.example.LinguaLearn.model;

import java.io.Serializable;

import com.google.cloud.firestore.annotation.DocumentId;

public class User implements Serializable {
    @DocumentId // Marks this field as the document ID
    private String uid;
    private String email;
    private String displayName;
    private Long createdAt;
    private Long lastLoginAt;
    private Integer loginCount;

    // Firestore requires a no-arg constructor
    public User() {
        // Initialize numeric fields to prevent NullPointerException
        this.loginCount = 0;
    }

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = System.currentTimeMillis();
        this.loginCount = 1;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    
    public Long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Long lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public Integer getLoginCount() { return loginCount; }
    public void setLoginCount(Integer loginCount) { this.loginCount = loginCount; }

    @Override
    public String toString() {
        return "User{" +
               "uid='" + uid + '\'' +
               ", email='" + email + '\'' +
               ", displayName='" + displayName + '\'' +
               ", createdAt=" + createdAt +
               ", lastLoginAt=" + lastLoginAt +
               ", loginCount=" + loginCount +
               '}';
    }
}