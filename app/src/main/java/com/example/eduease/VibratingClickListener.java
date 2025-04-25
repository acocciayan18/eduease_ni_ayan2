package com.example.eduease;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;

public class VibratingClickListener implements View.OnClickListener {
    private final View.OnClickListener originalClickListener;
    private final Context context;

    public VibratingClickListener(Context context, View.OnClickListener originalClickListener) {
        this.context = context;
        this.originalClickListener = originalClickListener;
    }

    @Override
    public void onClick(View v) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
        if (originalClickListener != null) {
            originalClickListener.onClick(v);
        }
    }
}
