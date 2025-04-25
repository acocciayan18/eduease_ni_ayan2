package com.example.eduease;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private List<Quiz> quizList;
    private QuizClickListener listener;

    public interface QuizClickListener {
        void onQuizClick(Quiz quiz);
    }

    public QuizAdapter(List<Quiz> quizList, QuizClickListener listener) {
        this.quizList = quizList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.quiz_item, parent, false);
        return new QuizViewHolder(v);
    }

    @Override
    public void onBindViewHolder(QuizViewHolder holder, int position) {
        Quiz currentQuiz = quizList.get(position);

        holder.titleView.setText(currentQuiz.getTitle());
        holder.descriptionView.setText(currentQuiz.getDescription());

        holder.itemView.setOnClickListener(v -> {
            if (currentQuiz.isFlash()) {
                Intent intent = new Intent(v.getContext(), TakeBonusFlash.class);
                intent.putExtra("quizId", currentQuiz.getId());
                v.getContext().startActivity(intent);
            } else {
                if (listener != null) {
                    listener.onQuizClick(currentQuiz);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return quizList.size();
    }

    public static class QuizViewHolder extends RecyclerView.ViewHolder {
        public TextView titleView;
        public TextView descriptionView;

        public QuizViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.title);
            descriptionView = itemView.findViewById(R.id.description);
        }
    }

    public void updateList(List<Quiz> newQuizList) {
        if (newQuizList != null) {
            quizList.clear();
            quizList.addAll(newQuizList);
            notifyDataSetChanged();
        }
    }
}
