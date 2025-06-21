package com.example.SE114_DoAn;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {

    private TextInputEditText etCardTitle, etDescription;
    private LinearLayout llMembersContainer, llAttachments, llSubmissions;
    private MaterialButton btnSave, btnCancel, btnAddAttachment, btnSubmitWork;
    private MaterialCardView cardAttachments, cardSubmissions;
    private TextView tvStartDate, tvEndDate;

    private Task task;
    private BoardViewModel viewModel;
    private FirebaseFirestore db;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault());
    private boolean isAdmin = false;

    private ActivityResultLauncher<String> filePickerLauncher;
    private String uploadContext = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        this.filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String fileName = getFileName(uri);
                        if ("attachment".equals(uploadContext)) {
                            viewModel.addFileAttachment(task.getTask_id(), fileName, uri);
                        } else if ("submission".equals(uploadContext)) {
                            viewModel.submitWorkForTask(task.getTask_id(), fileName, uri);
                        }
                    }
                });

        viewModel = new ViewModelProvider(this).get(BoardViewModel.class);
        db = FirebaseFirestore.getInstance();
        initViews();
        setupClickListeners();

        String taskId = getIntent().getStringExtra("taskId");

        if (taskId != null && !taskId.isEmpty()) {
            viewModel.getTaskById(taskId).observe(this, updatedTask -> {
                if (updatedTask != null) {
                    this.task = updatedTask;
                    bindDataToUI();
                    checkUserRole();
                }
            });
            observeAttachments(taskId);
            observeSubmissions(taskId);
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID của task.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        etCardTitle = findViewById(R.id.etCardTitle);
        etDescription = findViewById(R.id.etDescription);
        llMembersContainer = findViewById(R.id.llMembers);
        llAttachments = findViewById(R.id.llAttachments);
        llSubmissions = findViewById(R.id.llSubmissions);
        cardAttachments = findViewById(R.id.cardAttachments);
        cardSubmissions = findViewById(R.id.cardSubmissions);
        btnAddAttachment = findViewById(R.id.btnAddAttachment);
        btnSubmitWork = findViewById(R.id.btnSubmitWork);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        setEditMode(false);
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> finish());

        findViewById(R.id.deadline_container).setOnClickListener(v -> {
            if (isAdmin) {
                showDatePickerDialog();
            } else {
                Toast.makeText(this, "Bạn không có quyền chỉnh sửa.", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddAttachment.setOnClickListener(v -> {
            if (isAdmin) {
                showAddAttachmentDialog();
            } else {
                Toast.makeText(this, "Chỉ admin mới có thể thêm tài liệu.", Toast.LENGTH_SHORT).show();
            }
        });

        btnSubmitWork.setOnClickListener(v -> {
            uploadContext = "submission";
            filePickerLauncher.launch("*/*");
        });
    }

    private void bindDataToUI() {
        if (task == null) return;
        etCardTitle.setText(task.getTitle());
        etDescription.setText(task.getDescription());
        updateDates();
        updateMembersUI();
    }

    private void observeAttachments(String taskId) {
        viewModel.getDocumentsForTask(taskId).observe(this, this::updateAttachmentsUI);
    }

    private void observeSubmissions(String taskId) {
        viewModel.getSubmissionsForTask(taskId).observe(this, this::updateSubmissionsUI);
    }

    private void checkUserRole() {
        if (task == null || task.getGroup_id() == null) return;
        viewModel.getCurrentUserRoleForGroup(task.getGroup_id()).observe(this, role -> {
            this.isAdmin = (role != null && role.equals("admin"));
            setEditMode(this.isAdmin);
        });
    }

    private void setEditMode(boolean isEditable) {
        etCardTitle.setEnabled(isEditable);
        etDescription.setEnabled(isEditable);

        int adminControlsVisibility = isEditable ? View.VISIBLE : View.GONE;
        btnSave.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);

        cardAttachments.setVisibility(View.VISIBLE);
        cardSubmissions.setVisibility(View.VISIBLE);
        btnAddAttachment.setVisibility(adminControlsVisibility);

        updateMembersUI();
    }

    private void updateMembersUI() {
        if (llMembersContainer == null) return;
        llMembersContainer.removeAllViews();

        MaterialButton btnManageMembers = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        int memberCount = (task != null && task.getMembers() != null) ? task.getMembers().size() : 0;

        String buttonText = "Xem " + memberCount + " thành viên";
        if (isAdmin) {
            buttonText = "Xem và quản lý " + memberCount + " thành viên";
        }
        btnManageMembers.setText(buttonText);
        btnManageMembers.setAllCaps(false);
        btnManageMembers.setOnClickListener(v -> showMemberListDialog());
        llMembersContainer.addView(btnManageMembers);
    }

    private void updateAttachmentsUI(List<Document> documents) {
        if (llAttachments == null) return;
        llAttachments.removeAllViews();
        if (documents == null || documents.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Chưa có tài liệu nào.");
            llAttachments.addView(tv);
        } else {
            for (Document doc : documents) {
                llAttachments.addView(createAttachmentView(doc));
            }
        }
    }

    private View createAttachmentView(Document doc) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(0, 8, 0, 8);

        TextView linkView = new TextView(this);
        linkView.setText("🔗 " + doc.getName());
        linkView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        linkView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        linkView.setOnClickListener(v -> openUrl(doc.getFile_url()));
        itemLayout.addView(linkView);

        if (isAdmin) {
            ImageButton btnDelete = new ImageButton(this);
            btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnDelete.setBackground(null);
            btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog("tài liệu", doc.getName(), () -> viewModel.deleteAttachment(task.getTask_id(), doc)));
            itemLayout.addView(btnDelete);
        }
        return itemLayout;
    }

    private void updateSubmissionsUI(List<Submission> submissions) {
        if (llSubmissions == null) return;
        llSubmissions.removeAllViews();

        if (submissions == null || submissions.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Chưa có bài nộp nào.");
            llSubmissions.addView(tv);
        } else {
            for (Submission submission : submissions) {
                llSubmissions.addView(createSubmissionView(submission));
            }
        }
    }

    private View createSubmissionView(Submission submission) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(0, 8, 0, 8);

        TextView submissionView = new TextView(this);
        String uploaderId = submission.getSubmittedBy() != null ? submission.getSubmittedBy() : "";
        submissionView.setText("📄 " + submission.getFileName());
        submissionView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        submissionView.setOnClickListener(v -> openUrl(submission.getFileUrl()));
        itemLayout.addView(submissionView);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && (isAdmin || currentUser.getUid().equals(uploaderId))) {
            ImageButton btnDelete = new ImageButton(this);
            btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnDelete.setBackground(null);
            btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog("bài nộp", submission.getFileName(), () -> viewModel.deleteSubmission(task.getTask_id(), submission)));
            itemLayout.addView(btnDelete);
        }
        return itemLayout;
    }

    private void showMemberListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Danh sách thành viên");

        Context context = this;
        LinearLayout dialogLayout = new LinearLayout(context);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(40, 20, 40, 20);

        LinearLayout memberListLayout = new LinearLayout(context);
        memberListLayout.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(memberListLayout);

        dialogLayout.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        if (isAdmin) {
            Button btnAddNewMember = new Button(context);
            btnAddNewMember.setText("Thêm thành viên mới");
            btnAddNewMember.setOnClickListener(v -> showAddUserByEmailDialog());
            dialogLayout.addView(btnAddNewMember);
        }

        builder.setView(dialogLayout);
        AlertDialog memberDialog = builder.create();

        List<String> memberIds = (task.getMembers() != null) ? task.getMembers() : new ArrayList<>();

        if (memberIds.isEmpty()) {
            TextView tvNoMembers = new TextView(context);
            tvNoMembers.setText("Chưa có thành viên nào.");
            memberListLayout.addView(tvNoMembers);
        } else {
            db.collection("User").whereIn(FieldPath.documentId(), memberIds).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        memberListLayout.removeAllViews();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            User user = document.toObject(User.class);
                            memberListLayout.addView(createMemberViewForDialog(user, document.getId()));
                        }
                    })
                    .addOnFailureListener(e -> {
                        memberListLayout.removeAllViews();
                        TextView errorView = new TextView(context);
                        errorView.setText("Lỗi khi tải danh sách thành viên.");
                        memberListLayout.addView(errorView);
                    });
        }
        memberDialog.show();
    }

    private View createMemberViewForDialog(User user, String userId) {
        LinearLayout memberLayout = new LinearLayout(this);
        memberLayout.setOrientation(LinearLayout.HORIZONTAL);
        memberLayout.setGravity(Gravity.CENTER_VERTICAL);
        memberLayout.setPadding(0, 8, 0, 8);

        TextView tvMemberName = new TextView(this);
        tvMemberName.setText(user != null ? (user.getUsername() != null ? user.getUsername() : user.getEmail()) : userId);
        tvMemberName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        memberLayout.addView(tvMemberName);

        if (isAdmin) {
            ImageButton btnRemove = new ImageButton(this);
            btnRemove.setImageResource(android.R.drawable.ic_menu_delete);
            btnRemove.setBackground(null);
            btnRemove.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xóa Thành viên")
                        .setMessage("Bạn có chắc muốn xóa thành viên này khỏi task?")
                        .setPositiveButton("Xóa", (dialog, which) -> viewModel.removeUserFromTask(task.getTask_id(), userId))
                        .setNegativeButton("Hủy", null)
                        .show();
            });
            memberLayout.addView(btnRemove);
        }
        return memberLayout;
    }

    private void showAddUserByEmailDialog() {
        final EditText input = new EditText(this);
        input.setHint("Nhập email người cần thêm");
        new AlertDialog.Builder(this)
                .setTitle("Thêm Thành viên vào Group")
                .setMessage("Lưu ý: Thêm thành viên vào group sẽ cho phép họ thấy tất cả các task trong group này.")
                .setView(input)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        viewModel.addUserToGroup(task.getGroup_id(), email);
                        viewModel.addUserToTask(task.getTask_id(), email);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAddAttachmentDialog() {
        final CharSequence[] options = {"Thêm bằng Link", "Tải lên từ thiết bị", "Hủy"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn phương thức thêm tài liệu")
                .setItems(options, (dialog, item) -> {
                    if (options[item].equals("Thêm bằng Link")) {
                        showAddLinkDialog();
                    } else if (options[item].equals("Tải lên từ thiết bị")) {
                        uploadContext = "attachment";
                        filePickerLauncher.launch("*/*");
                    } else {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showAddLinkDialog() {
        // Code đầy đủ để hiển thị dialog thêm link
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "unknown_file";
    }

    private void saveTask() {
        if (task != null) {
            task.setTitle(String.valueOf(etCardTitle.getText()));
            task.setDescription(String.valueOf(etDescription.getText()));
            viewModel.updateTask(task);
            Toast.makeText(this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateDates() {
        if (task != null && task.getCreated_at() != null) {
            tvStartDate.setText(dateFormat.format(task.getCreated_at()));
        } else {
            tvStartDate.setText("Chưa có");
        }
        if (task != null && task.getDeadline() != null) {
            tvEndDate.setText(dateFormat.format(task.getDeadline()));
        } else {
            tvEndDate.setText("Chưa đặt");
        }
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        if (task.getDeadline() != null) {
            calendar.setTime(task.getDeadline());
        }
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            task.setDeadline(calendar.getTime());
            updateDates();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Không có đường dẫn hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở link hoặc file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog(String itemType, String itemName, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa " + itemType + " '" + itemName + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Hủy", null)
                .show();
    }
}