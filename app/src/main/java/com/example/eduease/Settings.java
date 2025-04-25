package com.example.eduease;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Settings extends AppCompatActivity {

    private ImageView profileImage;
    private EditText changePassword;
    private TextView account;
    private Button saveButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Initialize views
        profileImage = findViewById(R.id.profile_image);
        changePassword = findViewById(R.id.change_password);
        account = findViewById(R.id.account);
        saveButton = findViewById(R.id.save_btn);
        logoutButton = findViewById(R.id.logout_btn);

        // Load profile image
        loadProfileImage();

        // Display user information
        displayUserInfo();

        // Set up vibration feedback
        setupVibration(profileImage, changePassword, saveButton, logoutButton);

        // Set up the save button listener
        setupSaveButtonListener();

        // Set up the logout button listener
        setupLogoutButtonListener();

        // Adjust view for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .apply(new RequestOptions()
                            .circleCrop()
                            .override(192, 192)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.person)
                            .error(R.drawable.person))
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.person);
        }
    }

    private void displayUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            account.setText(user.getEmail());
            changePassword.setText("*******");
        } else {
            Toast.makeText(this, "No user information found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupVibration(View... views) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        for (View view : views) {
            view.setOnClickListener(v -> {
                if (vibrator != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(50);
                    }
                }
            });
        }
    }

    private void setupSaveButtonListener() {
        saveButton.setOnClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String newPassword = changePassword.getText().toString().trim();

                if (!newPassword.isEmpty()) {
                    user.updatePassword(newPassword).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            changePassword.setEnabled(false);
                            changePassword.setHint("Password Updated");
                        } else {
                            Toast.makeText(this, "Failed to update password: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No authenticated user found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLogoutButtonListener() {
        logoutButton.setOnClickListener(v -> {
            // Vibration feedback
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }

            // Log out the user
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Navigate to MainActivity with a flag
            Intent intent = new Intent(Settings.this, MainActivity.class);
            intent.putExtra("from_settings", true); // Add this extra
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // Finish current activity
            finish();
        });
    }
}
