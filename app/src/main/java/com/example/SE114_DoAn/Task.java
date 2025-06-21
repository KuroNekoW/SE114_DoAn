package com.example.SE114_DoAn;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList; // Import ArrayList
import java.util.Date;
import java.util.List; // Import List

public class Task implements Parcelable {
    @DocumentId
    private String task_id;

    private String title;
    private String description;
    private String group_id;
    private String status;
    private Date deadline;

    // THAY ĐỔI TỪ String sang List<String>
    private List<String> members = new ArrayList<>();

    @ServerTimestamp
    private Date created_at;

    public Task() {}

    // --- CẬP NHẬT PARCELABLE ---
    protected Task(Parcel in) {
        task_id = in.readString();
        title = in.readString();
        description = in.readString();
        group_id = in.readString();
        status = in.readString();

        long tmpDeadline = in.readLong();
        deadline = tmpDeadline == -1 ? null : new Date(tmpDeadline);

        // Đọc danh sách members từ Parcel
        members = in.createStringArrayList();

        long tmpCreatedAt = in.readLong();
        created_at = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(task_id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(group_id);
        dest.writeString(status);
        dest.writeLong(deadline != null ? deadline.getTime() : -1);

        // Ghi danh sách members vào Parcel
        dest.writeStringList(members);

        dest.writeLong(created_at != null ? created_at.getTime() : -1);
    }

    // --- KẾT THÚC CẬP NHẬT PARCELABLE ---

    @Override
    public int describeContents() { return 0; }

    public static final Creator<Task> CREATOR = new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel in) { return new Task(in); }
        @Override
        public Task[] newArray(int size) { return new Task[size]; }
    };

    // --- Getters và Setters (cập nhật cho members) ---
    public String getTask_id() { return task_id; }
    public void setTask_id(String task_id) { this.task_id = task_id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getGroup_id() { return group_id; }
    public void setGroup_id(String group_id) { this.group_id = group_id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    public Date getCreated_at() { return created_at; }
    public void setCreated_at(Date created_at) { this.created_at = created_at; }

    // Getter và Setter mới cho danh sách members
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
}