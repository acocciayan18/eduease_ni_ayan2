package com.example.eduease;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TakeQuiz extends AppCompatActivity {

    private LinearLayout qaContainer;
    private FirebaseFirestore db;
    private List<Map<String, String>> questionsList;
    private String quizId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_take_quiz);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        qaContainer = findViewById(R.id.qa_container);
        db = FirebaseFirestore.getInstance();

        // Get the quiz ID from the intent
        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (quizId != null) {
            loadQuizData(quizId);
        } else {
            Toast.makeText(this, "Quiz ID not provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set up the submit button
        Button submitButton = findViewById(R.id.submit_btn);
        submitButton.setOnClickListener(v -> handleSubmit());
    }

    @SuppressLint("SetTextI18n")
    private void loadQuizData(String quizId) {
        db.collection("quizzes").document(quizId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Display the quiz title
                        String title = documentSnapshot.getString("title");
                        if (title != null) {
                            TextView quizTitle = findViewById(R.id.quiz_title);
                            quizTitle.setText(title);
                        }

                        // Clear the container
                        qaContainer.removeAllViews();

                        // Collect all questions into a list
                        questionsList = new ArrayList<>();
                        for (int i = 1; ; i++) {
                            Map<String, String> qa = (Map<String, String>) documentSnapshot.get("Question " + i);
                            if (qa == null) break;
                            questionsList.add(qa);
                        }

                        // Shuffle the questions
                        Collections.shuffle(questionsList);

                        // Add shuffled questions to the UI
                        int questionNumber = 1;
                        for (Map<String, String> qa : questionsList) {
                            addQuestionBlock(questionNumber++, qa.get("question"));
                        }
                    } else {
                        Toast.makeText(this, "Quiz not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addQuestionBlock(int questionNumber, String question) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View qaBlock = inflater.inflate(R.layout.question_and_answer, qaContainer, false);

        TextView questionNumberView = qaBlock.findViewById(R.id.question_number);
        EditText questionField = qaBlock.findViewById(R.id.question_field);
        EditText answerField = qaBlock.findViewById(R.id.answer_field);

        // Set the question text and make it read-only
        questionNumberView.setText("#" + questionNumber + " Question:");
        questionField.setText(question);
        questionField.setFocusable(false);
        questionField.setClickable(false);
        questionField.setFocusableInTouchMode(false);
        questionField.setBackground(null);
        questionField.setTextIsSelectable(false);

        answerField.setHint("Enter your answer here");

        // Tag the block with the question's index
        qaBlock.setTag(questionNumber - 1); // Zero-based index matching questionsList

        // Listen for text changes in the answer field
        answerField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !answerField.getText().toString().trim().isEmpty()) {
                moveBlockToBottom(qaBlock);
            }
        });

        qaContainer.addView(qaBlock);
    }

    /**
     * Moves the given question block to the bottom of the container.
     */
    private void moveBlockToBottom(View qaBlock) {
        qaContainer.removeView(qaBlock);
        qaContainer.addView(qaBlock);

        // Smoothly scroll to the first unanswered question
        qaContainer.post(() -> qaContainer.getChildAt(0).requestFocus());
    }

    private void handleSubmit() {
        boolean allAnswered = true;

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View child = qaContainer.getChildAt(i);
            EditText answerField = child.findViewById(R.id.answer_field);
            if (answerField.getText().toString().trim().isEmpty()) {
                allAnswered = false;
                break;
            }
        }

        if (!allAnswered) {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Unanswered Questions")
                    .setMessage("You have unanswered questions. Are you sure you want to submit?")
                    .setPositiveButton("Yes", (dialog, which) -> calculateScore())
                    .setNegativeButton("No", null)
                    .show();
        } else {
            calculateScore();
        }
    }

    @SuppressLint("SetTextI18n")
    private void calculateScore() {
        int score = 0;
        StringBuilder resultBuilder = new StringBuilder();

        for (int i = 0; i < qaContainer.getChildCount(); i++) {
            View child = qaContainer.getChildAt(i);
            TextView questionField = child.findViewById(R.id.question_field);
            EditText answerField = child.findViewById(R.id.answer_field);

            // Get the question index from the tag
            int questionIndex = (int) child.getTag();

            String userAnswer = answerField.getText().toString().trim(); // Trim user input
            Map<String, String> questionData = questionsList.get(questionIndex); // Use the index to fetch the correct question
            String correctAnswer = questionData.get("answer").trim(); // Trim correct answer

            // Case-insensitive comparison
            if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                score++;
            }

            // Append question, correct answer, and user's answer to results
            resultBuilder.append("Q: ").append(questionField.getText().toString()).append("\n");
            resultBuilder.append("Your Answer: ").append(userAnswer.isEmpty() ? "No Answer" : userAnswer).append("\n");
            resultBuilder.append("Correct Answer: ").append(correctAnswer).append("\n\n");
        }

        // Calculate the percentage score
        int totalQuestions = questionsList.size();
        float rawPercentage = (score / (float) totalQuestions) * 100;

        // Apply the rounding logic
        float roundedPercentage = rawPercentage >= 75
                ? (float) Math.floor(rawPercentage)
                : (float) Math.ceil(rawPercentage);

        // Select the sound to play
        int soundResId = roundedPercentage >= 75 ? R.raw.pass : R.raw.fail;

        // Play the sound
        MediaPlayer mediaPlayer = MediaPlayer.create(this, soundResId);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> mp.release()); // Release resources after playback

        // Show the results
        new AlertDialog.Builder(this)
                .setTitle("Quiz Results")
                .setMessage("Your Score: " + score + "/" + totalQuestions + "\nPercentage: " + roundedPercentage + "%\n\n" + resultBuilder.toString())
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }
}
