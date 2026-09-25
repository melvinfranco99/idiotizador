package com.melvin.idiotizador;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_MIC = 1;
    private static final int DEFAULT_DELAY_MS = 500;

    private final DelayEngine engine = new DelayEngine();
    private Button toggle;
    private TextView delayLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        engine.setDelayMs(DEFAULT_DELAY_MS);
    }

    private LinearLayout buildUi() {
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#1A1027"));
        int pad = (int) (24 * dp);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("IDIOTIZADOR");
        title.setTextColor(Color.WHITE);
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Ponte auriculares y habla.\nOirás tu voz con retardo.");
        hint.setTextColor(Color.parseColor("#B8A9CC"));
        hint.setTextSize(16);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, (int) (12 * dp), 0, (int) (40 * dp));
        root.addView(hint);

        toggle = new Button(this);
        toggle.setTextColor(Color.WHITE);
        toggle.setTextSize(22);
        toggle.setTypeface(Typeface.DEFAULT_BOLD);
        int size = (int) (200 * dp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.gravity = Gravity.CENTER;
        toggle.setLayoutParams(lp);
        toggle.setOnClickListener(v -> onToggle());
        root.addView(toggle);

        delayLabel = new TextView(this);
        delayLabel.setTextColor(Color.WHITE);
        delayLabel.setTextSize(18);
        delayLabel.setGravity(Gravity.CENTER);
        delayLabel.setPadding(0, (int) (40 * dp), 0, (int) (8 * dp));
        root.addView(delayLabel);

        // Retardo ajustable de 100 a 1500 ms en pasos de 50 ms
        SeekBar seek = new SeekBar(this);
        seek.setMax(28);
        seek.setProgress((DEFAULT_DELAY_MS - 100) / 50);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int ms = 100 + p * 50;
                engine.setDelayMs(ms);
                updateDelayLabel(ms);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        root.addView(seek, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        updateDelayLabel(DEFAULT_DELAY_MS);
        updateButton();
        return root;
    }

    private void updateDelayLabel(int ms) {
        delayLabel.setText(String.format("Retardo: %.2f s", ms / 1000f));
    }

    private void updateButton() {
        boolean on = engine.isRunning();
        toggle.setText(on ? "PARAR" : "EMPEZAR");
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor(on ? "#D32F2F" : "#7B1FA2"));
        toggle.setBackground(bg);
        if (on) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onToggle() {
        if (engine.isRunning()) {
            engine.stop();
        } else if (hasMicPermission()) {
            engine.start();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        updateButton();
    }

    private boolean hasMicPermission() {
        return Build.VERSION.SDK_INT < 23
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_MIC) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                engine.start();
                updateButton();
            } else {
                Toast.makeText(this, "Se necesita el micrófono para funcionar",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Al salir de la app se deja de capturar el micrófono
        if (engine.isRunning()) {
            engine.stop();
            updateButton();
        }
    }
}
