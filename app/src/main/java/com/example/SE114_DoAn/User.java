package com.example.SE114_DoAn;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {
    private String username;
    private String email;

    @ServerTimestamp
    private Date created_at;

    public User() {
        // Cần constructor rỗng cho Firestore
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }
}