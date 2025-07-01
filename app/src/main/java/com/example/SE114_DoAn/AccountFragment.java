package com.example.SE114_DoAn;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class AccountFragment extends Fragment {

    private TextView tvAccountName, tvAccountEmail;
    private ImageView imageProfile;
    private Button imageProfileBtn;

    private SharedPreferences sharedPreferences;
    private StorageReference storageReference;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BoardViewModel viewModel; // Thêm ViewModel để gọi hàm cập nhật

    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String TAG = "AccountFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        viewModel = new ViewModelProvider(requireActivity()).get(BoardViewModel.class);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, requireActivity().MODE_PRIVATE);
        storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference profileRef= storageReference.child("usersProfilePic/"+mAuth.getCurrentUser().getUid()+"/profile.jpg");
        profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(imageProfile);
            }
        });

        tvAccountName = view.findViewById(R.id.tvAccountName);
        tvAccountEmail = view.findViewById(R.id.tvAccountEmail);
        ImageButton btnAccountMenu = view.findViewById(R.id.btnAccountMenu);
        imageProfile = view.findViewById(R.id.imageProfile);
        imageProfileBtn = view.findViewById(R.id.imageProfileBtn);

        loadAndListenUserProfile();

        // THÊM SỰ KIỆN CLICK VÀO TÊN ĐỂ CHỈNH SỬA
        tvAccountName.setOnClickListener(v -> showEditUsernameDialog());


        // Listener cho menu
        btnAccountMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(requireContext(), btnAccountMenu);
            popupMenu.getMenu().add(0, 1, 0, "Thêm tài khoản");
            popupMenu.getMenu().add(0, 2, 0, "Đăng xuất");

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    Toast.makeText(requireContext(), "Chức năng chưa được triển khai", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (item.getItemId() == 2) {
                    logoutUser();
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });
        //Nút đổi ảnh
        imageProfileBtn.setOnClickListener(v -> {
            Intent openGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityIntent.launch(openGallery);
        });


        return view;
    }
    //Đổi ảnh
    private final ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Intent image=result.getData();
                        Uri imageUri = image.getData();
                        uploadImagetoFirebase(imageUri);
                    }
                }
            });

    private void uploadImagetoFirebase(Uri imageUri) {
        StorageReference imageRef = storageReference.child("usersProfilePic/"+mAuth.getCurrentUser().getUid()+"/profile.jpg");
        imageRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.get().load(uri).into(imageProfile);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getActivity(), "Image không được upload", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAndListenUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            tvAccountEmail.setText(email);
            String uid = currentUser.getUid();

            // Dùng addSnapshotListener để lắng nghe thay đổi thời gian thực
            db.collection("User").document(uid)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Lỗi khi lắng nghe thông tin người dùng", error);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            tvAccountName.setText(username);
                        } else {
                            // Fallback nếu document không tồn tại
                            tvAccountName.setText(email != null ? email.substring(0, email.indexOf('@')) : "Người dùng");
                        }
                    });
        } else {
            tvAccountName.setText("Khách");
            tvAccountEmail.setText("Vui lòng đăng nhập");
        }
    }


    private void showEditUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chỉnh sửa tên hiển thị");

        // Tạo một EditText cho người dùng nhập tên mới
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nhập tên mới");
        // Điền tên hiện tại vào EditText
        input.setText(tvAccountName.getText().toString());
        builder.setView(input);

        // Thiết lập các nút
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (!newUsername.isEmpty()) {
                // Gọi phương thức trong ViewModel để cập nhật
                viewModel.updateUsername(newUsername);
            } else {
                Toast.makeText(getContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void logoutUser() {
        mAuth.signOut();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_LOGGED_IN, false);
        editor.apply();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}