package com.example.eduease;

public class Question {
    private String questionText;
    private String answerText;

    // Default constructor (required for Firestore to deserialize data)
    public Question() {}

    // Constructor with parameters
    public Question(String questionText, String answerText) {
        this.questionText = questionText;
        this.answerText = answerText;
    }

    // Getters and setters
    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
}
