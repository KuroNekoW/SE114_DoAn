package com.example.SE114_DoAn;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Khởi tạo các view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Tìm NavHostFragment và lấy NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment with ID nav_host_fragment not found in activity_main.xml");
        }
        NavController navController = navHostFragment.getNavController();

        // Cấu hình BottomNavigationView với NavController
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_board, R.id.nav_notifications, R.id.nav_account, R.id.nav_chat)
                .build();
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Thêm listener để xử lý navigation thủ công
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Lấy ID của mục được chọn
            int destinationId = item.getItemId();

            // Kiểm tra nếu đã ở đích đến hiện tại, không làm gì cả
            if (navController.getCurrentDestination().getId() == destinationId) {
                return true;
            }

            // Xử lý navigation thủ công
            try {
                // Pop back stack để tránh chồng chất fragment
                navController.popBackStack(destinationId, true);
                // Navigate đến đích mới
                navController.navigate(destinationId);
                return true;
            } catch (IllegalArgumentException e) {
                // Nếu đích không thể tìm thấy, trả về false
                return false;
            }
        });
    }
}