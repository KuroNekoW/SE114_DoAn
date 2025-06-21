package com.example.SE114_DoAn;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import androidx.navigation.fragment.NavHostFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class GroupChatFragment extends Fragment {

    private static final String TAG = "GroupChatFragment";

    private RecyclerView rvChatMessages;
    private EditText etMessageInput;
    private ImageButton btnSendMessage;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CollectionReference messagesRef;

    private String groupId;
    private String groupName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getString("GROUP_ID");
            groupName = getArguments().getString("GROUP_NAME");
        }
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_chat, container, false);

        if (groupId == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy group chat.", Toast.LENGTH_SHORT).show();
            return view;
        }

        Toolbar toolbar = view.findViewById(R.id.toolbar_group_chat);
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar); // Đặt toolbar này làm action bar chính
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(groupName); // Đặt tên nhóm làm tiêu đề
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút back (<-)
                activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        messagesRef = db.collection("Group").document(groupId).collection("Messages");

        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etMessageInput = view.findViewById(R.id.etMessageInput);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);

        setupRecyclerView();
        loadMessages();

        btnSendMessage.setOnClickListener(v -> sendMessage());

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Kiểm tra xem có phải nút "Home" (nút back trên toolbar) được nhấn không
        if (item.getItemId() == android.R.id.home) {
            // Sử dụng NavController để quay lại màn hình trước đó
            NavHostFragment.findNavController(this).navigateUp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentUserId = (currentUser != null) ? currentUser.getUid() : "";

        // Truyền ID người dùng vào adapter
        chatAdapter = new ChatAdapter(chatMessages, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void loadMessages() {
        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(getActivity(), (snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi khi lắng nghe tin nhắn.", error);
                        return;
                    }
                    if (snapshots == null) return;

                    chatMessages.clear();
                    chatMessages.addAll(snapshots.toObjects(ChatMessage.class));
                    chatAdapter.notifyDataSetChanged();
                    if (!chatMessages.isEmpty()) {
                        rvChatMessages.scrollToPosition(chatMessages.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (messageText.isEmpty() || currentUser == null) {
            return;
        }

        // Lấy thông tin user từ collection "User"
        db.collection("User").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = "Người dùng"; // Tên mặc định
                    if (documentSnapshot.exists()) {
                        // Ưu tiên lấy username từ Firestore
                        username = documentSnapshot.getString("username");
                    } else {
                        // Nếu không có, dùng email làm tên tạm
                        username = currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "Người dùng";
                    }

                    // Tạo và gửi tin nhắn với username đã lấy được
                    ChatMessage message = new ChatMessage(currentUser.getUid(), username, messageText);
                    messagesRef.add(message)
                            .addOnSuccessListener(documentReference -> etMessageInput.setText(""))
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Gửi tin nhắn thất bại.", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi khi lấy thông tin người dùng.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Không thể lấy user profile", e);
                });
    }
}