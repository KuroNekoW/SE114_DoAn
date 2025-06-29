package com.example.SE114_DoAn;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import android.net.Uri;

import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class BoardViewModel extends ViewModel {
    private static final String TAG = "BoardViewModel";

    // LiveData cho màn hình Board
    private final MutableLiveData<List<Group>> groups = new MutableLiveData<>();
    private final MutableLiveData<Map<String, List<Task>>> groupTasks = new MutableLiveData<>();

    // LiveData mới cho màn hình Chat Lobby
    private final MutableLiveData<List<GroupChatInfo>> groupChatInfos = new MutableLiveData<>();

    private final MutableLiveData<String> operationStatus = new MutableLiveData<>();

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseUser currentUser;
    private final Map<String, GroupChatInfo> groupChatInfoMap = new ConcurrentHashMap<>();
    private final Map<String, List<Task>> groupTasksMap = new ConcurrentHashMap<>();

    public BoardViewModel() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();
        loadInitialData();
    }

    // --- Getters ---
    public LiveData<List<Group>> getGroups() { return groups; }
    public LiveData<Map<String, List<Task>>> getGroupTasks() { return groupTasks; }
    public LiveData<List<GroupChatInfo>> getGroupChatInfos() { return groupChatInfos; }
    public LiveData<String> getOperationStatus() { return operationStatus; }

    private void loadInitialData() {
        if (currentUser == null) {
            operationStatus.setValue("Lỗi: Người dùng chưa đăng nhập.");
            return;
        }

        db.collection("Member")
                .whereEqualTo("user_id", currentUser.getUid())
                .addSnapshotListener((memberSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading member data", error);
                        return;
                    }
                    if (memberSnapshots == null || memberSnapshots.isEmpty()) {
                        groups.setValue(new ArrayList<>());
                        groupChatInfos.setValue(new ArrayList<>());
                        groupTasks.setValue(new HashMap<>());
                        return;
                    }

                    List<String> groupIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : memberSnapshots) {
                        String groupId = doc.getString("group_id");
                        if (groupId != null) {
                            groupIds.add(groupId);
                        }
                    }

                    if (!groupIds.isEmpty()) {
                        fetchGroupsAndRelatedData(groupIds);
                    } else {
                        groups.setValue(new ArrayList<>());
                        groupChatInfos.setValue(new ArrayList<>());
                        groupTasks.setValue(new HashMap<>());
                    }
                });
    }

    private void fetchGroupsAndRelatedData(List<String> groupIds) {
        groupTasksMap.clear();
        db.collection("Group").whereIn(FieldPath.documentId(), groupIds)
                .addSnapshotListener((groupSnapshots, error) -> {
                    if (error != null) { return; }
                    if (groupSnapshots == null) return;

                    List<Group> fetchedGroups = groupSnapshots.toObjects(Group.class);
                    groups.setValue(fetchedGroups);

                    groupChatInfoMap.clear();
                    for (Group group : fetchedGroups) {
                        groupChatInfoMap.put(group.getGroup_id(), new GroupChatInfo(group));
                        listenForLastMessage(group.getGroup_id());
                        listenForTasks(group.getGroup_id()); // Tách logic load task
                    }
                });
    }

    private void listenForLastMessage(String groupId) {
        db.collection("Group").document(groupId).collection("Messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((messageSnapshots, error) -> {
                    if (error != null) { return; }

                    GroupChatInfo info = groupChatInfoMap.get(groupId);
                    if (info == null) return;

                    if (messageSnapshots != null && !messageSnapshots.isEmpty()) {
                        info.setLastMessage(messageSnapshots.getDocuments().get(0).toObject(ChatMessage.class));
                    } else {
                        info.setLastMessage(null);
                    }

                    updateGroupChatInfosLiveData();
                });
    }

    private void listenForTasks(String groupId) {
        db.collection("Task").whereEqualTo("group_id", groupId)
                .orderBy("created_at", Query.Direction.ASCENDING)
                .addSnapshotListener((taskSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for tasks", error);
                        return;
                    }
                    if (taskSnapshots != null) {
                        List<Task> tasks = taskSnapshots.toObjects(Task.class);
                        groupTasksMap.put(groupId, tasks);
                        groupTasks.postValue(new HashMap<>(groupTasksMap));
                    }
                });
    }

    private void updateGroupChatInfosLiveData() {
        groupChatInfos.postValue(new ArrayList<>(groupChatInfoMap.values()));
    }

    public void addGroup(String groupName) {
        if (currentUser == null) return;
        Group newGroup = new Group(groupName, "", currentUser.getUid());
        db.collection("Group").add(newGroup)
                .addOnSuccessListener(documentReference -> {
                    String groupId = documentReference.getId();
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("user_id", currentUser.getUid());
                    memberData.put("group_id", groupId);
                    memberData.put("role", "admin");
                    memberData.put("joined_at", new Date());
                    db.collection("Member").document(currentUser.getUid() + "_" + groupId).set(memberData)
                            .addOnSuccessListener(aVoid -> operationStatus.setValue("Tạo nhóm '" + groupName + "' thành công."))
                            .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi thêm thành viên."));
                })
                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi tạo nhóm: " + e.getMessage()));
    }

    public void addTaskToGroup(String groupId, String taskTitle) {
        if (currentUser == null || groupId == null) return;
        Task newTask = new Task();
        newTask.setTitle(taskTitle);
        newTask.setGroup_id(groupId);
        newTask.setStatus("to-do");
        newTask.getMembers().add(currentUser.getUid()); // Tự động thêm người tạo vào task
        db.collection("Task").add(newTask)
                .addOnSuccessListener(docRef -> operationStatus.setValue("Thêm task '" + taskTitle + "' thành công."))
                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi thêm task."));
    }

    public void deleteTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) return;
        db.collection("Task").document(taskId).delete()
                .addOnSuccessListener(aVoid -> operationStatus.setValue("Xóa task thành công."))
                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi xóa task."));
    }

    public void updateTask(Task task) {
        if (task == null || task.getTask_id() == null) return;
        db.collection("Task").document(task.getTask_id()).set(task)
                .addOnSuccessListener(aVoid -> operationStatus.setValue("Cập nhật task thành công."))
                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi cập nhật task."));
    }

    public LiveData<Task> getTaskById(String taskId) {
        MutableLiveData<Task> taskData = new MutableLiveData<>();
        db.collection("Task").document(taskId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to task", error);
                        operationStatus.setValue("Lỗi khi tải chi tiết task.");
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        taskData.setValue(snapshot.toObject(Task.class));
                    }
                });
        return taskData;
    }

    public void addUserToTask(String taskId, String userEmail) {
        if (taskId == null || userEmail == null || userEmail.trim().isEmpty()) {
            operationStatus.setValue("Email không hợp lệ.");
            return;
        }
        db.collection("User").whereEqualTo("email", userEmail.trim()).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        operationStatus.setValue("Không tìm thấy người dùng với email: " + userEmail);
                    } else {
                        String targetUserId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("Task").document(taskId)
                                .update("members", FieldValue.arrayUnion(targetUserId))
                                .addOnSuccessListener(aVoid -> operationStatus.setValue("Thêm thành viên thành công!"))
                                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi thêm thành viên."));
                    }
                })
                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi tìm kiếm người dùng."));
    }

    public void removeUserFromTask(String taskId, String userId) {
        if (taskId == null || userId == null) return;
        db.collection("Task").document(taskId)
                .update("members", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> operationStatus.setValue("Xóa thành viên thành công!"))
                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi xóa thành viên."));
    }

    public void addUserToGroup(String groupId, String userEmail) {
        if (groupId == null || userEmail == null || userEmail.trim().isEmpty()) {
            operationStatus.setValue("Email hoặc Group ID không hợp lệ.");
            return;
        }

        // Tìm user trong collection "User" bằng email
        db.collection("User").whereEqualTo("email", userEmail.trim()).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        operationStatus.setValue("Không tìm thấy người dùng với email: " + userEmail);
                    } else {
                        // Lấy user_id của người dùng được tìm thấy
                        String targetUserId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Bước 2: Tạo một bản ghi mới trong collection "Member"
                        Map<String, Object> memberData = new HashMap<>();
                        memberData.put("user_id", targetUserId);
                        memberData.put("group_id", groupId);
                        memberData.put("role", "member"); // Gán vai trò là thành viên
                        memberData.put("joined_at", new Date());

                        db.collection("Member").document(targetUserId + "_" + groupId).set(memberData)
                                .addOnSuccessListener(aVoid -> operationStatus.setValue("Thêm thành viên vào nhóm thành công!"))
                                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi thêm thành viên vào nhóm."));
                    }
                })
                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi tìm kiếm người dùng."));
    }

    public void updateUsername(String newUsername) {
        if (currentUser == null) {
            operationStatus.setValue("Người dùng chưa đăng nhập.");
            return;
        }
        if (newUsername == null || newUsername.trim().isEmpty()) {
            operationStatus.setValue("Tên người dùng không được để trống.");
            return;
        }

        String uid = currentUser.getUid();
        String email = currentUser.getEmail();
        // Tạo một Map để chứa dữ liệu cần cập nhật
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", newUsername.trim());

        db.collection("User").document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> operationStatus.setValue("Cập nhật tên thành công!"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi cập nhật username", e);
                    operationStatus.setValue("Cập nhật tên thất bại.");
                });
    }

    public LiveData<String> getCurrentUserRoleForGroup(String groupId) {
        MutableLiveData<String> userRole = new MutableLiveData<>();
        if (currentUser == null || groupId == null) {
            userRole.setValue(null);
            return userRole;
        }
        String memberDocId = currentUser.getUid() + "_" + groupId;
        db.collection("Member").document(memberDocId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi khi lấy vai trò người dùng", error);
                        userRole.setValue(null);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        userRole.setValue(snapshot.getString("role"));
                    } else {
                        userRole.setValue(null); // Không phải là thành viên
                    }
                });
        return userRole;
    }

    public LiveData<List<Document>> getDocumentsForTask(String taskId) {
        MutableLiveData<List<Document>> documentsData = new MutableLiveData<>();
        db.collection("Task").document(taskId).collection("Documents")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi khi nghe tài liệu", error);
                        return;
                    }
                    if (snapshots != null) {
                        documentsData.setValue(snapshots.toObjects(Document.class));
                    }
                });
        return documentsData;
    }

    public void addLinkAttachment(String taskId, String linkName, String linkUrl) {
        if (currentUser == null || taskId == null) return;
        Document newDoc = new Document(linkName, linkUrl, "link", currentUser.getUid());
        db.collection("Task").document(taskId).collection("Documents")
                .add(newDoc)
                .addOnSuccessListener(docRef -> operationStatus.setValue("Thêm link thành công."))
                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi thêm link."));
    }

    public void addFileAttachment(String taskId, String fileName, Uri fileUri) {
        if (currentUser == null || taskId == null || fileUri == null) {
            operationStatus.setValue("Dữ liệu không hợp lệ.");
            return;
        }

        operationStatus.setValue("Đang tải lên file...");
        StorageReference fileRef = storage.getReference().child("attachments/" + taskId + "/" + fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                        String fileType = getFileTypeFromFileName(fileName);
                        Document newDoc = new Document(fileName, downloadUrl.toString(), fileType, currentUser.getUid());
                        db.collection("Task").document(taskId).collection("Documents")
                                .add(newDoc)
                                .addOnSuccessListener(docRef -> operationStatus.setValue("Thêm tài liệu thành công."))
                                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi lưu thông tin tài liệu."));
                    });
                })
                .addOnFailureListener(e -> operationStatus.setValue("Tải file lên thất bại."))
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    operationStatus.postValue("Đang tải lên... " + (int) progress + "%");
                });
    }

    private String getFileTypeFromFileName(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1);
        }
        return extension.toLowerCase();
    }

    public LiveData<List<Submission>> getSubmissionsForTask(String taskId) {
        MutableLiveData<List<Submission>> submissionsData = new MutableLiveData<>();
        db.collection("Task").document(taskId).collection("Submissions")
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) { return; }
                    if (snapshots != null) {
                        submissionsData.setValue(snapshots.toObjects(Submission.class));
                    }
                });
        return submissionsData;
    }

    /**
     * Xử lý việc thành viên nộp bài.
     */
    public void submitWorkForTask(String taskId, String fileName, Uri fileUri) {
        if (currentUser == null || taskId == null || fileUri == null) return;

        operationStatus.setValue("Đang nộp bài...");
        // Lưu bài nộp vào một thư mục riêng trên Storage
        StorageReference fileRef = storage.getReference().child("submissions/" + taskId + "/" + currentUser.getUid() + "/" + fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                        Submission submission = new Submission(fileName, downloadUrl.toString(), currentUser.getUid());
                        db.collection("Task").document(taskId).collection("Submissions")
                                .add(submission)
                                .addOnSuccessListener(docRef -> operationStatus.setValue("Nộp bài thành công."))
                                .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi lưu thông tin bài nộp."));
                    });
                })
                .addOnFailureListener(e -> operationStatus.setValue("Nộp bài thất bại."));
    }

    public void deleteAttachment(String taskId, Document document) {
        if (document == null || document.getFile_url() == null || document.getId() == null) return;

        // Lấy tham chiếu đến file trên Storage từ URL
        StorageReference fileRef = storage.getReferenceFromUrl(document.getFile_url());

        fileRef.delete().addOnSuccessListener(aVoid -> {
            // Xóa file trên Storage thành công, giờ xóa document trên Firestore
            db.collection("Task").document(taskId).collection("Documents")
                    .document(document.getId())
                    .delete()
                    .addOnSuccessListener(aVoid2 -> operationStatus.setValue("Xóa tài liệu thành công."))
                    .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi xóa thông tin tài liệu."));
        }).addOnFailureListener(e -> {
            operationStatus.setValue("Lỗi khi xóa file trên Storage.");
            Log.e(TAG, "Could not delete file from Storage", e);
        });
    }

    public void deleteSubmission(String taskId, Submission submission) {
        if (submission == null || submission.getFileUrl() == null || submission.getId() == null) return;

        StorageReference fileRef = storage.getReferenceFromUrl(submission.getFileUrl());

        fileRef.delete().addOnSuccessListener(aVoid -> {
            db.collection("Task").document(taskId).collection("Submissions")
                    .document(submission.getId())
                    .delete()
                    .addOnSuccessListener(aVoid2 -> operationStatus.setValue("Xóa bài nộp thành công."))
                    .addOnFailureListener(e -> operationStatus.setValue("Lỗi khi xóa thông tin bài nộp."));
        }).addOnFailureListener(e -> {
            operationStatus.setValue("Lỗi khi xóa file bài nộp.");
            Log.e(TAG, "Could not delete submission file from Storage", e);
        });
    }
}