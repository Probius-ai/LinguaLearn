package com.example.LinguaLearn.model;

import com.google.cloud.firestore.annotation.DocumentId;
import java.util.Date;

public class Friend {
    @DocumentId
    private String id;
    private String userId;
    private String friendId;
    private String status; // "pending", "accepted", "rejected"
    private Date createdAt;
    private Date updatedAt;

    // Firestore requires a no-arg constructor
    public Friend() {}

    public Friend(String userId, String friendId) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = "pending";
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFriendId() { return friendId; }
    public void setFriendId(String friendId) { this.friendId = friendId; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = new Date();
    }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Friend{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", friendId='" + friendId + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}