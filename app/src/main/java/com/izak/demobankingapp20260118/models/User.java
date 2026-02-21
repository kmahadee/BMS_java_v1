package com.izak.demobankingapp20260118.models;


import com.google.firebase.database.PropertyName;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String userId;
    private String email;
    private String role;
    private boolean approved;
    private long createdAt;

    public User() {
    }

    public User(String userId, String email, String role, boolean approved, long createdAt) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.approved = approved;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }



    @PropertyName("isApproved")
    public boolean isApproved() {
        return approved;
    }

    @PropertyName("isApproved")
    public void setApproved(boolean approved) {
        this.approved = approved;
    }
    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", isApproved=" + approved +
                ", createdAt=" + createdAt +
                '}';
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("email", email);
        map.put("role", role);
        map.put("isApproved", approved);
        map.put("createdAt", createdAt);
        return map;
    }
}
