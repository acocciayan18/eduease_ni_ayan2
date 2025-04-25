package com.example.eduease;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;

public class CreateAccount extends AppCompatActivity {

    private static final String TAG = "CreateAccountActivity";
    private static final int RC_SIGN_IN = 100;
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)[A-Za-z\\d]{7,}$";
    private static final int VIBRATION_DURATION = 50;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);
        applyEdgeToEdgePadding();
        initializeFirebase();
        setupButtonClickListeners();
    }

    private void applyEdgeToEdgePadding() {
        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile() // Ensure profile info is fetched
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupButtonClickListeners() {
        Button signUpButton = findViewById(R.id.sign_up_button);
        ImageButton googleSignInButton = findViewById(R.id.google_sign_in_button);
        TextView loginTextButton = findViewById(R.id.login_text_button);
        EditText passwordInput = findViewById(R.id.password_input);
        EditText retypePasswordInput = findViewById(R.id.retype_password_input);

        setupToggleForPasswordField(passwordInput);
        setupToggleForPasswordField(retypePasswordInput);

        signUpButton.setOnClickListener(view -> {
            vibratePhone();
            handleSignUp();
        });

        googleSignInButton.setOnClickListener(view -> {
            vibratePhone();
            handleGoogleSignIn();
        });

        loginTextButton.setOnClickListener(view -> {
            vibratePhone();
            handleLogin();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupToggleForPasswordField(EditText passwordField) {
        passwordField.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableEndIndex = 2; // Index for drawableEnd
                if (event.getRawX() >= (passwordField.getRight() - Objects.requireNonNull(passwordField.getCompoundDrawables()[drawableEndIndex]).getBounds().width())) {
                    togglePasswordVisibility(passwordField);
                    passwordField.performClick(); // Ensure performClick is invoked for accessibility
                    return true;
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility(EditText passwordField) {
        boolean isPasswordVisible = passwordField.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordField.setInputType(isPasswordVisible ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordField.setCompoundDrawablesWithIntrinsicBounds(
                passwordField.getCompoundDrawables()[0],
                passwordField.getCompoundDrawables()[1],
                getDrawable(isPasswordVisible ? R.drawable.show_password : R.drawable.hide_password),
                passwordField.getCompoundDrawables()[3]
        );
        passwordField.setSelection(passwordField.getText().length()); // Maintain cursor position
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(VIBRATION_DURATION);
            }
        }
    }

    private void handleSignUp() {
        EditText emailInput = findViewById(R.id.email_input);
        EditText passwordInput = findViewById(R.id.password_input);
        EditText retypePasswordInput = findViewById(R.id.retype_password_input);

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String retypePassword = retypePasswordInput.getText().toString().trim();

        if (!validateInputs(email, password, retypePassword)) return;

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(this, "Account created successfully! Welcome, " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            navigateToHome(user);
                        }
                    } else {
                        Log.w(TAG, "handleSignUp: Failure", task.getException());
                        Toast.makeText(this, "Account creation failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password, String retypePassword) {
        EditText emailInput = findViewById(R.id.email_input);
        EditText passwordInput = findViewById(R.id.password_input);
        EditText retypePasswordInput = findViewById(R.id.retype_password_input);

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            emailInput.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        if (!password.matches(PASSWORD_PATTERN)) {
            passwordInput.setError("Password must be at least 7 characters, and include an uppercase letter, lowercase letter, and a number.");
            passwordInput.requestFocus();
            return false;
        }

        if (!password.equals(retypePassword)) {
            retypePasswordInput.setError("Passwords do not match");
            retypePasswordInput.requestFocus();
            return false;
        }

        return true;
    }

    private void handleGoogleSignIn() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            ActivityOptions options = ActivityOptions.makeBasic();
            startActivityForResult(signInIntent, RC_SIGN_IN, options.toBundle());
        });
    }

    private void handleLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        finish();
    }

    private void navigateToHome(FirebaseUser user) {
        Intent homeIntent = new Intent(this, Home.class);
        homeIntent.putExtra("user_email", user.getEmail());
        homeIntent.putExtra("user_name", user.getDisplayName());

        // Handle profile image for email sign-in
        String profileImageUrl = null;
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && account.getPhotoUrl() != null) {
            profileImageUrl = account.getPhotoUrl().toString();
        } else {
            // Default profile image for email sign-in users
            profileImageUrl = "@drawable/person"; // Replace with a constant or default image logic
        }
        homeIntent.putExtra("PROFILE_IMAGE_URL", profileImageUrl);

        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
        startActivity(homeIntent, options.toBundle());
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        if (account == null) {
            Log.w(TAG, "firebaseAuthWithGoogle: GoogleSignInAccount is null!");
            Toast.makeText(this, "Google sign in failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            String profileImageUrl = null;
                            if (account.getPhotoUrl() != null) {
                                profileImageUrl = account.getPhotoUrl().toString();
                            }

                            Toast.makeText(this, "Sign in successful! Welcome, " + user.getDisplayName() + "!", Toast.LENGTH_LONG).show();

                            Intent homeIntent = new Intent(this, Home.class);
                            homeIntent.putExtra("PROFILE_IMAGE_URL", profileImageUrl);
                            homeIntent.putExtra("user_email", user.getEmail());
                            homeIntent.putExtra("user_name", user.getDisplayName());
                            startActivity(homeIntent);
                            finish();
                        }
                    } else {
                        Log.w(TAG, "firebaseAuthWithGoogle: signInWithCredential failed", task.getException());
                        Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
