package com.example.LinguaLearn.model;

import com.google.cloud.firestore.annotation.DocumentId;

public class User {
    @DocumentId // Marks this field as the document ID
    private String uid;
    private String email;
    private String displayName;
    // Add other fields as needed, e.g., registration timestamp

    // Firestore requires a no-arg constructor
    public User() {}

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    @Override
    public String toString() {
        return "User{" +
               "uid='" + uid + '\'' +
               ", email='" + email + '\'' +
               ", displayName='" + displayName + '\'' +
               '}';
    }
}
