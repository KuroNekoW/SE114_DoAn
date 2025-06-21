package com.example.SE114_DoAn;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatMessage {
    private String userId;
    private String username;
    private String message;

    // Dùng annotation này để Firestore tự điền thời gian của server
    @ServerTimestamp
    private Date timestamp;

    public ChatMessage() {
        // Cần constructor rỗng cho Firebase
    }

    // Timestamp sẽ được server tự động thêm vào.
    public ChatMessage(String userId, String username, String message) {
        this.userId = userId;
        this.username = username;
        this.message = message;
    }

    // Getters and Setters (không thay đổi)
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}