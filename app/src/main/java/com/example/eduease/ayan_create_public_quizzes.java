package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ayan_create_public_quizzes extends AppCompatActivity {

    private LinearLayout qaContainer;
    private Vibrator vibrator;
    private EditText quizTitle;
    private EditText quizDescription;
    private DatabaseReference publicDbRef;
    private FirebaseApp secondaryApp;
    private String quizId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ayan_activity_create_public_quiz);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        quizTitle = findViewById(R.id.quiz_title);
        quizDescription = findViewById(R.id.quiz_description);
        qaContainer = findViewById(R.id.qa_container);

        // Initialize secondary Firebase App
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                .setProjectId("edu-ease-ni-ayan")
                .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                .build();

        try {
            secondaryApp = FirebaseApp.initializeApp(this, options, "secondary");
        } catch (IllegalStateException e) {
            secondaryApp = FirebaseApp.getInstance("secondary");
        }

        publicDbRef = FirebaseDatabase.getInstance(secondaryApp).getReference("public_quizzes");

        MaterialButton saveButton = findViewById(R.id.save_btn);
        saveButton.setOnClickListener(v -> saveToSecondaryDatabase());

        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (quizId != null) {
            loadQuizData(quizId);
        } else {
            for (int i = 0; i < 3; i++) {
                addQuestionAnswerBlock();
            }
        }
    }

    private View addQuestionAnswerBlock() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View qaBlock = inflater.inflate(R.layout.question_and_answer, qaContainer, false);

        monitorQAChanges(qaBlock);

        ImageButton addButton = qaBlock.findViewById(R.id.add_qa);
        addButton.setOnClickListener(v -> {
            vibrate();
            addQuestionAnswerBlock();
        });

        ImageButton deleteButton = qaBlock.findViewById(R.id.delete_qa);
        deleteButton.setOnClickListener(v -> {
            vibrate();
            qaContainer.removeView(qaBlock);
            updateDeleteButtons();
        });

        qaContainer.addView(qaBlock);
        updateDeleteButtons();
        return qaBlock;
    }

    private void monitorQAChanges(View qaBlock) {
        EditText questionInput = qaBlock.findViewById(R.id.question_field);
        EditText answerInput = qaBlock.findViewById(R.id.answer_field);

        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (!hasFocus) {
                rearrangeBlocks();
            }
        };

        questionInput.setOnFocusChangeListener(focusChangeListener);
        answerInput.setOnFocusChangeListener(focusChangeListener);
    }

    private void rearrangeBlocks() {
        int childCount = qaContainer.getChildCount();
        List<View> filledBlocks = new ArrayList<>();
        List<View> emptyBlocks = new ArrayList<>();

        for (int i = 0; i < childCount; i++) {
            View block = qaContainer.getChildAt(i);
            EditText questionInput = block.findViewById(R.id.question_field);
            EditText answerInput = block.findViewById(R.id.answer_field);

            if (questionInput.getText().toString().trim().isEmpty() || answerInput.getText().toString().trim().isEmpty()) {
                emptyBlocks.add(block);
            } else {
                filledBlocks.add(block);
            }
        }

        qaContainer.removeAllViews();
        for (View block : emptyBlocks) {
            qaContainer.addView(block);
        }
        for (View block : filledBlocks) {
            qaContainer.addView(block);
        }
        updateDeleteButtons();
    }

    @SuppressLint("SetTextI18n")
    private void updateDeleteButtons() {
        int childCount = qaContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View qaBlock = qaContainer.getChildAt(i);
            ImageButton deleteButton = qaBlock.findViewById(R.id.delete_qa);
            deleteButton.setEnabled(childCount > 1);
        }
    }

    private void saveToSecondaryDatabase() {
        String title = quizTitle.getText().toString().trim();
        String description = quizDescription.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Title and Description cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> quizData = new HashMap<>();
        quizData.put("creatorId", currentUser.getUid());
        quizData.put("title", title);
        quizData.put("description", description);
        quizData.put("timestamp", System.currentTimeMillis());
        quizData.put("type", "public"); // <-- Added this line

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View qaBlock = qaContainer.getChildAt(i);
            EditText questionInput = qaBlock.findViewById(R.id.question_field);
            EditText answerInput = qaBlock.findViewById(R.id.answer_field);

            String question = questionInput.getText().toString().trim();
            String answer = answerInput.getText().toString().trim();

            if (!question.isEmpty() && !answer.isEmpty()) {
                Map<String, String> qa = new HashMap<>();
                qa.put("question", question);
                qa.put("answer", answer);
                quizData.put("Question_" + (i + 1), qa);
            }
        }

        String quizKey = publicDbRef.push().getKey();
        if (quizKey != null) {
            publicDbRef.child(quizKey)
                    .setValue(quizData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Public quiz saved successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, Home.class));
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save public quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }


    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void loadQuizData(String quizId) {
        // Future method if you want to edit existing quiz
    }
}
