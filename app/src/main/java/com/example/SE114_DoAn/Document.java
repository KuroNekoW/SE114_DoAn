package com.example.SE114_DoAn;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Document {
    private String name;
    private String file_url;
    private String file_type;
    private String uploaded_by;
    private String id;
    @ServerTimestamp
    private Date created_at;

    public Document() {}

    public Document(String name, String file_url, String file_type, String uploaded_by) {
        this.name = name;
        this.file_url = file_url;
        this.file_type = file_type;
        this.uploaded_by = uploaded_by;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFile_url() { return file_url; }
    public void setFile_url(String file_url) { this.file_url = file_url; }
    public String getFile_type() { return file_type; }
    public void setFile_type(String file_type) { this.file_type = file_type; }
    public String getUploaded_by() { return uploaded_by; }
    public void setUploaded_by(String uploaded_by) { this.uploaded_by = uploaded_by; }
    public Date getCreated_at() { return created_at; }
    public void setCreated_at(Date created_at) { this.created_at = created_at; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}