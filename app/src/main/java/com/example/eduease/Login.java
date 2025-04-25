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

public class Login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 100;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private ImageButton googleLoginButton;
    private TextView signUpTextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);







        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        applyEdgeToEdgePadding();

        // Initialize Firebase Auth and Google Sign-In
        firebaseAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize UI components
        initializeUI();

        // Set up listeners for buttons and input fields
        setListeners();

        // Enable toggle functionality for password field
        setupToggleForPasswordField(passwordInput);
    }

    private void applyEdgeToEdgePadding() {
        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initializeUI() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        googleLoginButton = findViewById(R.id.google_login_button);
        signUpTextButton = findViewById(R.id.signup_text_button);
    }

    private void setListeners() {
        loginButton.setOnClickListener(v -> {
            vibratePhone();
            signInWithEmail();
        });

        googleLoginButton.setOnClickListener(v -> {
            vibratePhone();
            signInWithGoogle();
        });

        signUpTextButton.setOnClickListener(v -> {
            vibratePhone();
            openSignUpActivity();
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

    @SuppressLint("UseCompatLoadingForDrawables")
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
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        } else {
            Log.w(TAG, "Vibrator service unavailable");
        }
    }

    private void signInWithEmail() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter both email and password");
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            updateUI(user);
                        }
                    } else {
                        Log.w(TAG, "LoginWithEmail:failure", task.getException());
                        showToast("Login failed. Check credentials.");
                    }
                });
    }

    private void signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            ActivityOptions options = ActivityOptions.makeBasic();
            startActivityForResult(signInIntent, RC_SIGN_IN, options.toBundle());
        });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            updateUI(user);
                        }
                    } else {
                        Log.w(TAG, "firebaseAuthWithGoogle:failure", task.getException());
                        showToast("Google Login failed. Please try again.");
                    }
                });
    }

    private void openSignUpActivity() {
        Intent createAccountIntent = new Intent(Login.this, CreateAccount.class);
        createAccountIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(createAccountIntent);
        finish();
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent homeIntent = new Intent(Login.this, Home.class);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);

            // Pass Google profile picture URL if available
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null && account.getPhotoUrl() != null) {
                homeIntent.putExtra("PROFILE_IMAGE_URL", account.getPhotoUrl().toString());
            }

            homeIntent.putExtra("SKIP_MUSIC", true); // Skip music when navigating to Home
            startActivity(homeIntent, options.toBundle());
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google Login failed", e);
                showToast("Google Login failed. Please try again.");
            }
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show());
    }
}
