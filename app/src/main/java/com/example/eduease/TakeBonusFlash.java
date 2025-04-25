package com.example.eduease;

import com.google.firebase.FirebaseApp;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.FirebaseOptions;

public class TakeBonusFlash extends AppCompatActivity {

    private TextView quizTitleTextView;
    private TextView quizDescriptionTextView;
    private GridLayout bonusPointsContainer;

    private int totalQuestions = 0;
    private int answeredQuestions = 0;


    private int totalBonusPoints = 0; // Store the total bonus points
    private TextView totalBonusPointsTextView; // Reference to the TextView for displaying total points


    private LinearLayout qaContainer;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ayan_activity_take_bonus_flash);

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                .setProjectId("edu-ease-ni-ayan")
                .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                .build();

        try {
            FirebaseApp.initializeApp(this, options, "bonusFlashApp");
        } catch (IllegalStateException ignored) {
        }

        quizTitleTextView = findViewById(R.id.quiz_title);
        quizDescriptionTextView = findViewById(R.id.quiz_description);
        bonusPointsContainer = findViewById(R.id.bonus_points_container);
        qaContainer = findViewById(R.id.qa_container);

        totalBonusPointsTextView = findViewById(R.id.total_bonus_points);


        Intent intent = getIntent();
        String quizId = intent.getStringExtra("quizId");
        if (quizId == null || quizId.isEmpty()) return;

        loadQuizDetails(quizId);
    }

    private void loadQuizDetails(String quizId) {
        try {
            FirebaseDatabase bonusDb = FirebaseDatabase.getInstance(FirebaseApp.getInstance("bonusFlashApp"));
            DatabaseReference quizRef = bonusDb.getReference("bonus_quizzes").child(quizId);

            quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String title = snapshot.child("title").getValue(String.class);
                        String description = snapshot.child("description").getValue(String.class);
                        quizTitleTextView.setText(title != null ? title : "Title not found");
                        quizDescriptionTextView.setText(description != null ? description : "Description not found");
                        loadBonusPoints(snapshot);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadBonusPoints(DataSnapshot dataSnapshot) {

        totalQuestions = 0;
        answeredQuestions = 0;
        for (DataSnapshot bonusQASnapshot : dataSnapshot.getChildren()) {
            try {
                Integer bonusPoints = bonusQASnapshot.child("bonusPoints").getValue(Integer.class);
                String question = bonusQASnapshot.child("question").getValue(String.class);
                String answer = bonusQASnapshot.child("answer").getValue(String.class);

                if (bonusPoints != null && question != null && answer != null) {
                    totalQuestions++;
                    View cardView = getLayoutInflater().inflate(R.layout.item_bonus_question, bonusPointsContainer, false);

                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = 0;
                    params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                    params.setMargins(8, 8, 8, 8);
                    cardView.setLayoutParams(params);

                    TextView bonusPointsTextView = cardView.findViewById(R.id.bonus_points);
                    bonusPointsTextView.setText("" + bonusPoints);

                    // Center the text and make it bold
                    bonusPointsTextView.setGravity(Gravity.CENTER);  // Center the text horizontally and vertically
                    bonusPointsTextView.setTypeface(null, Typeface.BOLD);  // Make the text bold

                    bonusPointsContainer.addView(cardView);

                    // Set click listener to show question dialog
                    cardView.setOnClickListener(v -> showBonusQuestionDialog(question, answer, bonusPoints, cardView));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    private void showBonusQuestionDialog(String question, String answer, Integer bonusPoints, View cardView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(TakeBonusFlash.this);
        View dialogView = getLayoutInflater().inflate(R.layout.bonus_question_dialog, null);

        TextView questionTextView = dialogView.findViewById(R.id.question_text);
        EditText answerInput = dialogView.findViewById(R.id.answer_input);
        TextView bonusPointsTextView = dialogView.findViewById(R.id.bonus_points_text);
        Button submitButton = dialogView.findViewById(R.id.submit_button);

        questionTextView.setText(question);
        bonusPointsTextView.setText("Bonus Points: " + bonusPoints);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        submitButton.setOnClickListener(v -> {
            String userAnswer = answerInput.getText().toString().trim();

            if (!userAnswer.isEmpty()) {
                cardView.setEnabled(false);            // Disable card so it can't be clicked again
                cardView.setAlpha(0.5f);               // Visually dim it

                if (userAnswer.equalsIgnoreCase(answer)) {
                    totalBonusPoints += bonusPoints;
                    totalBonusPointsTextView.setText("Total Bonus Points: " + totalBonusPoints);
                    Toast.makeText(TakeBonusFlash.this, "Correct! You earned " + bonusPoints + " bonus points.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TakeBonusFlash.this, "Oops! That’s incorrect. Better luck next time!", Toast.LENGTH_SHORT).show();
                }

                answeredQuestions++;
                dialog.dismiss();

                if (answeredQuestions == totalQuestions) {
                    showCompletionDialog();
                }
            } else {
                Toast.makeText(TakeBonusFlash.this, "Please provide an answer.", Toast.LENGTH_SHORT).show();
            }
        });


        dialog.show();
    }

    private void showCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You have answered all bonus questions!\nTotal Bonus Points: " + totalBonusPoints)
                .setCancelable(false)
                .setPositiveButton("Finish", (dialog, which) -> finish())
                .show();
    }









}
