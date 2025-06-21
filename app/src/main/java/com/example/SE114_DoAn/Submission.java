package com.example.SE114_DoAn;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Submission {
    private String fileName;
    private String fileUrl;
    private String submittedBy; // user_id của người nộp
    private String id;
    @ServerTimestamp
    private Date submittedAt;

    public Submission() {}

    public Submission(String fileName, String fileUrl, String submittedBy) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.submittedBy = submittedBy;
    }

    // Getters and Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public Date getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}