package com.example.SE114_DoAn;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Group {
    @DocumentId
    private String group_id; // Firestore sẽ tự động gán ID của document vào đây

    private String name;
    private String description;
    private String created_by; // user_id của người tạo

    @ServerTimestamp
    private Date created_at; // Firestore sẽ tự động gán thời gian của server

    // Constructors
    public Group() {}

    public Group(String name, String description, String created_by) {
        this.name = name;
        this.description = description;
        this.created_by = created_by;
    }

    // Getters and Setters
    public String getGroup_id() { return group_id; }
    public void setGroup_id(String group_id) { this.group_id = group_id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreated_by() { return created_by; }
    public void setCreated_by(String created_by) { this.created_by = created_by; }
    public Date getCreated_at() { return created_at; }
    public void setCreated_at(Date created_at) { this.created_at = created_at; }
}