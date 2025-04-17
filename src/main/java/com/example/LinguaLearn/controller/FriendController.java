package com.example.LinguaLearn.controller;

import com.example.LinguaLearn.model.Friend;
import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.FriendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);

    @Autowired
    private FriendService friendService;

    /**
     * Get all friends of the current user
     */
    @GetMapping
    public ResponseEntity<?> getUserFriends(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        try {
            List<Map<String, Object>> friends = friendService.getUserFriends(currentUser.getUid());
            return ResponseEntity.ok(Map.of("friends", friends));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error getting user friends", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get friends: " + e.getMessage()));
        }
    }

    /**
     * Get all pending friend requests for the current user
     */
    @GetMapping("/requests")
    public ResponseEntity<?> getPendingRequests(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        try {
            List<Map<String, Object>> requests = friendService.getPendingFriendRequests(currentUser.getUid());
            return ResponseEntity.ok(Map.of("requests", requests));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error getting friend requests", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get friend requests: " + e.getMessage()));
        }
    }

    /**
     * Send a friend request
     */
    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        String friendId = requestBody.get("friendId");
        if (friendId == null || friendId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Friend ID is required"));
        }

        // Prevent sending request to self
        if (friendId.equals(currentUser.getUid())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot send friend request to yourself"));
        }

        try {
            Friend request = friendService.sendFriendRequest(currentUser.getUid(), friendId);
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Could not send friend request"));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Friend request sent successfully",
                    "request", request
            ));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error sending friend request", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send friend request: " + e.getMessage()));
        }
    }

    /**
     * Accept a friend request
     */
    @PostMapping("/accept/{requestId}")
    public ResponseEntity<?> acceptFriendRequest(
            @PathVariable String requestId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        try {
            Friend request = friendService.acceptFriendRequest(requestId);
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Friend request not found"));
            }

            // Verify that the request is addressed to the current user
            if (!request.getFriendId().equals(currentUser.getUid())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to accept this request"));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Friend request accepted",
                    "request", request
            ));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error accepting friend request", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to accept friend request: " + e.getMessage()));
        }
    }

    /**
     * Reject a friend request
     */
    @PostMapping("/reject/{requestId}")
    public ResponseEntity<?> rejectFriendRequest(
            @PathVariable String requestId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        try {
            Friend request = friendService.rejectFriendRequest(requestId);
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Friend request not found"));
            }

            // Verify that the request is addressed to the current user
            if (!request.getFriendId().equals(currentUser.getUid())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to reject this request"));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Friend request rejected",
                    "request", request
            ));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error rejecting friend request", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reject friend request: " + e.getMessage()));
        }
    }

    /**
     * Remove a friend
     */
    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<?> removeFriend(
            @PathVariable String friendshipId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        try {
            boolean removed = friendService.removeFriend(friendshipId);
            if (!removed) {
                return ResponseEntity.badRequest().body(Map.of("error", "Friendship not found"));
            }

            return ResponseEntity.ok(Map.of("message", "Friend removed successfully"));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error removing friend", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove friend: " + e.getMessage()));
        }
    }

    /**
     * Search for users to add as friends
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam String query,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        try {
            List<Map<String, Object>> results = friendService.searchUsers(currentUser.getUid(), query);
            return ResponseEntity.ok(Map.of("results", results));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error searching users", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search users: " + e.getMessage()));
        }
    }

    /**
     * Check if two users are friends
     */
    @GetMapping("/check/{otherUserId}")
    public ResponseEntity<?> checkFriendship(
            @PathVariable String otherUserId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        try {
            boolean areFriends = friendService.areFriends(currentUser.getUid(), otherUserId);
            return ResponseEntity.ok(Map.of("areFriends", areFriends));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error checking friendship", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check friendship: " + e.getMessage()));
        }
    }
}