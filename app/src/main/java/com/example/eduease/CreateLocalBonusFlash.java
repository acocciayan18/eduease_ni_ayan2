package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.util.HashMap;
import java.util.Map;

public class CreateLocalBonusFlash extends AppCompatActivity {

    private EditText bonusQuizTitle, bonusQuizDescription;
    private LinearLayout bonusQaContainer;
    private Vibrator vibrator;
    private DatabaseReference secondaryDb;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ayan_activity_create_local_bonus_flash);

        bonusQuizTitle = findViewById(R.id.flashquiz_input_title);
        bonusQuizDescription = findViewById(R.id.flashquiz_input_description);
        bonusQaContainer = findViewById(R.id.flashquiz_qa_block_container);
        MaterialButton saveBtn = findViewById(R.id.flashquiz_button_submit);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();

            FirebaseApp secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "secondary");
            secondaryDb = FirebaseDatabase.getInstance(secondaryApp).getReference("bonus_quizzes"); // <- changed node
            Log.d("CreateLocalBonusFlash", "Secondary Firebase initialized with Realtime Database");
        } catch (IllegalStateException e) {
            Log.e("CreateLocalBonusFlash", "Firebase initialization error: " + e.getMessage());
        }

        saveBtn.setOnClickListener(v -> {
            Log.d("CreateLocalBonusFlash", "Save button clicked");
            saveQuiz();
        });

        for (int i = 0; i < 3; i++) {
            addBonusQABlock();
        }
    }

    private void saveQuiz() {
        String title = bonusQuizTitle.getText().toString().trim();
        String desc = bonusQuizDescription.getText().toString().trim();

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Title and Description can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> quizData = new HashMap<>();
        quizData.put("title", title);
        quizData.put("description", desc);
        quizData.put("creatorId", user.getUid());
        quizData.put("flash", true);
        quizData.put("type", "local"); // <- added type=local field

        for (int i = 0; i < bonusQaContainer.getChildCount(); i++) {
            View qaView = bonusQaContainer.getChildAt(i);
            EditText questionField = qaView.findViewWithTag("bonus_question_field");
            EditText answerField = qaView.findViewWithTag("bonus_answer_field");
            EditText bonusField = qaView.findViewWithTag("bonus_points_field");

            String question = questionField.getText().toString().trim();
            String answer = answerField.getText().toString().trim();
            String pointsStr = bonusField.getText().toString().trim();
            int points = pointsStr.isEmpty() ? 0 : Integer.parseInt(pointsStr);

            if (!question.isEmpty() && !answer.isEmpty()) {
                Map<String, Object> qa = new HashMap<>();
                qa.put("question", question);
                qa.put("answer", answer);
                qa.put("bonusPoints", points);
                quizData.put("BonusQA" + (i + 1), qa);
            }
        }

        Log.d("CreateLocalBonusFlash", "Saving quiz to secondary DB");

        secondaryDb.push().setValue(quizData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Local Bonus Flash quiz saved!", Toast.LENGTH_SHORT).show();
                    Log.d("CreateLocalBonusFlash", "Quiz saved successfully");
                    startActivity(new Intent(this, Home.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("CreateLocalBonusFlash", "Error saving quiz: " + e.getMessage());
                });
    }

    private void addBonusQABlock() {
        LinearLayout qaBlock = new LinearLayout(this);
        qaBlock.setOrientation(LinearLayout.VERTICAL);
        qaBlock.setPadding(16, 16, 16, 16);

        EditText questionField = new EditText(this);
        questionField.setHint("Enter Question");
        questionField.setId(View.generateViewId());
        questionField.setTag("bonus_question_field");

        EditText answerField = new EditText(this);
        answerField.setHint("Enter Answer");
        answerField.setId(View.generateViewId());
        answerField.setTag("bonus_answer_field");

        EditText bonusPointsField = new EditText(this);
        bonusPointsField.setHint("Bonus Points");
        bonusPointsField.setInputType(InputType.TYPE_CLASS_NUMBER);
        bonusPointsField.setId(View.generateViewId());
        bonusPointsField.setTag("bonus_points_field");

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        ImageButton addBtn = new ImageButton(this);
        addBtn.setImageResource(R.drawable.ic_add);
        addBtn.setBackgroundColor(Color.TRANSPARENT);
        addBtn.setOnClickListener(v -> {
            vibrate();
            addBonusQABlock();
        });

        ImageButton deleteBtn = new ImageButton(this);
        deleteBtn.setImageResource(R.drawable.ic_delete);
        deleteBtn.setBackgroundColor(Color.TRANSPARENT);
        deleteBtn.setOnClickListener(v -> {
            vibrate();
            bonusQaContainer.removeView(qaBlock);
            updateDeleteButtons();
        });

        buttonLayout.addView(addBtn);
        buttonLayout.addView(deleteBtn);

        qaBlock.addView(questionField);
        qaBlock.addView(answerField);
        qaBlock.addView(bonusPointsField);
        qaBlock.addView(buttonLayout);

        bonusQaContainer.addView(qaBlock);
        updateDeleteButtons();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateDeleteButtons() {
        int count = bonusQaContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View qaView = bonusQaContainer.getChildAt(i);
            LinearLayout buttonLayout = (LinearLayout) ((LinearLayout) qaView).getChildAt(3);
            ImageButton deleteBtn = (ImageButton) buttonLayout.getChildAt(1);
            deleteBtn.setEnabled(count > 1);
        }
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }
}
