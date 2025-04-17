package com.example.LinguaLearn.service;

import com.example.LinguaLearn.model.Friend;
import com.example.LinguaLearn.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private static final Logger logger = LoggerFactory.getLogger(FriendService.class);
    private static final String FRIENDS_COLLECTION = "friends";

    @Autowired
    private Firestore firestore;

    @Autowired
    private UserService userService;

    /**
     * Send a friend request
     */
    public Friend sendFriendRequest(String userId, String friendId) throws ExecutionException, InterruptedException {
        // Validate that both users exist
        User user = userService.getUser(userId);
        User friendUser = userService.getUser(friendId);

        if (user == null || friendUser == null) {
            logger.error("Cannot send friend request: User or friend not found");
            return null;
        }

        // Check if request already exists
        List<Friend> existingRequests = getFriendRequestsBetweenUsers(userId, friendId);
        if (!existingRequests.isEmpty()) {
            logger.info("Friend request already exists between users {} and {}", userId, friendId);
            return existingRequests.get(0);
        }

        // Create new friend request
        Friend request = new Friend(userId, friendId);
        DocumentReference docRef = firestore.collection(FRIENDS_COLLECTION).document();
        request.setId(docRef.getId());

        ApiFuture<WriteResult> result = docRef.set(request);
        result.get(); // Wait for operation to complete

        logger.info("Friend request sent from {} to {}", userId, friendId);
        return request;
    }

    /**
     * Get all friend requests between two users
     */
    private List<Friend> getFriendRequestsBetweenUsers(String userId, String friendId) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(FRIENDS_COLLECTION)
                .whereIn("userId", Arrays.asList(userId, friendId))
                .whereIn("friendId", Arrays.asList(userId, friendId));

        ApiFuture<QuerySnapshot> future = query.get();
        QuerySnapshot snapshot = future.get();

        List<Friend> requests = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Friend request = doc.toObject(Friend.class);
            requests.add(request);
        }

        return requests;
    }

    /**
     * Accept a friend request
     */
    public Friend acceptFriendRequest(String requestId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(FRIENDS_COLLECTION).document(requestId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot snapshot = future.get();

        if (!snapshot.exists()) {
            logger.error("Friend request not found: {}", requestId);
            return null;
        }

        Friend request = snapshot.toObject(Friend.class);
        request.setStatus("accepted");

        ApiFuture<WriteResult> result = docRef.update("status", "accepted", "updatedAt", new Date());
        result.get(); // Wait for operation to complete

        logger.info("Friend request accepted: {}", requestId);
        return request;
    }

    /**
     * Reject a friend request
     */
    public Friend rejectFriendRequest(String requestId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(FRIENDS_COLLECTION).document(requestId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot snapshot = future.get();

        if (!snapshot.exists()) {
            logger.error("Friend request not found: {}", requestId);
            return null;
        }

        Friend request = snapshot.toObject(Friend.class);
        request.setStatus("rejected");

        ApiFuture<WriteResult> result = docRef.update("status", "rejected", "updatedAt", new Date());
        result.get(); // Wait for operation to complete

        logger.info("Friend request rejected: {}", requestId);
        return request;
    }

    /**
     * Delete a friend connection
     */
    public boolean removeFriend(String requestId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(FRIENDS_COLLECTION).document(requestId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot snapshot = future.get();

        if (!snapshot.exists()) {
            logger.error("Friend connection not found: {}", requestId);
            return false;
        }

        ApiFuture<WriteResult> result = docRef.delete();
        result.get(); // Wait for operation to complete

        logger.info("Friend connection removed: {}", requestId);
        return true;
    }

    /**
     * Get all pending friend requests for a user
     */
    public List<Map<String, Object>> getPendingFriendRequests(String userId) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("friendId", userId)
                .whereEqualTo("status", "pending");

        ApiFuture<QuerySnapshot> future = query.get();
        QuerySnapshot snapshot = future.get();

        List<Map<String, Object>> requests = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Friend request = doc.toObject(Friend.class);

            // Get sender user details
            User sender = userService.getUser(request.getUserId());
            if (sender != null) {
                Map<String, Object> requestDetails = new HashMap<>();
                requestDetails.put("id", request.getId());
                requestDetails.put("sender", sender);
                requestDetails.put("createdAt", request.getCreatedAt());
                requests.add(requestDetails);
            }
        }

        return requests;
    }

    /**
     * Get all friends of a user
     */
    public List<Map<String, Object>> getUserFriends(String userId) throws ExecutionException, InterruptedException {
        List<Map<String, Object>> friendsList = new ArrayList<>();

        // Get requests where user is the sender and status is accepted
        Query query1 = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "accepted");

        ApiFuture<QuerySnapshot> future1 = query1.get();

        // Get requests where user is the recipient and status is accepted
        Query query2 = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("friendId", userId)
                .whereEqualTo("status", "accepted");

        ApiFuture<QuerySnapshot> future2 = query2.get();

        // Process first query results
        for (DocumentSnapshot doc : future1.get().getDocuments()) {
            Friend friendship = doc.toObject(Friend.class);
            User friendUser = userService.getUser(friendship.getFriendId());

            if (friendUser != null) {
                Map<String, Object> friendDetails = new HashMap<>();
                friendDetails.put("id", friendship.getId());
                friendDetails.put("user", friendUser);
                friendDetails.put("createdAt", friendship.getCreatedAt());
                friendsList.add(friendDetails);
            }
        }

        // Process second query results
        for (DocumentSnapshot doc : future2.get().getDocuments()) {
            Friend friendship = doc.toObject(Friend.class);
            User friendUser = userService.getUser(friendship.getUserId());

            if (friendUser != null) {
                Map<String, Object> friendDetails = new HashMap<>();
                friendDetails.put("id", friendship.getId());
                friendDetails.put("user", friendUser);
                friendDetails.put("createdAt", friendship.getCreatedAt());
                friendsList.add(friendDetails);
            }
        }

        return friendsList;
    }

    /**
     * Check if two users are friends
     */
    public boolean areFriends(String userId1, String userId2) throws ExecutionException, InterruptedException {
        List<Friend> requests = getFriendRequestsBetweenUsers(userId1, userId2);

        for (Friend request : requests) {
            if ("accepted".equals(request.getStatus())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Search users by email or display name (for adding friends)
     */
    public List<Map<String, Object>> searchUsers(String userId, String query) throws ExecutionException, InterruptedException {
        // This is a simplified implementation. In a real app, you might want to:
        // 1. Use a proper search index like Algolia or Elasticsearch
        // 2. Implement pagination
        // 3. Add more sophisticated filtering

        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        query = query.toLowerCase().trim();

        // Get all users and filter on the client side (not efficient for large user bases)
        CollectionReference usersRef = firestore.collection("users");
        ApiFuture<QuerySnapshot> future = usersRef.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Map<String, Object>> results = new ArrayList<>();
        for (DocumentSnapshot doc : documents) {
            User user = doc.toObject(User.class);

            // Skip the current user
            if (user.getUid().equals(userId)) {
                continue;
            }

            boolean matchesEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(query);
            boolean matchesName = user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(query);

            if (matchesEmail || matchesName) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("uid", user.getUid());
                userInfo.put("email", user.getEmail());
                userInfo.put("displayName", user.getDisplayName());

                // Check if there's already a friend request
                List<Friend> requests = getFriendRequestsBetweenUsers(userId, user.getUid());
                if (!requests.isEmpty()) {
                    userInfo.put("friendRequestStatus", requests.get(0).getStatus());
                    userInfo.put("friendRequestId", requests.get(0).getId());
                } else {
                    userInfo.put("friendRequestStatus", "none");
                }

                results.add(userInfo);
            }
        }

        return results;
    }
}