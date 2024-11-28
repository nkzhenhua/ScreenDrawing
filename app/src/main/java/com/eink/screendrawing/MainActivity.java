package com.eink.screendrawing;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private WindowManager windowManager;
    private DrawingView drawingView;
    private View menuView;
    private boolean isDrawingEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
        } else {
            initializeOverlay();
        }
    }

    private void initializeOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Initialize drawing view
        drawingView = new DrawingView(this);
        WindowManager.LayoutParams drawParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(drawingView, drawParams);

        // Initialize menu
        initializeMenu();
    }

    private void initializeMenu() {
        LayoutInflater inflater = LayoutInflater.from(this);
        menuView = inflater.inflate(R.layout.floating_menu, null);

        WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.TOP | Gravity.START;

        windowManager.addView(menuView, menuParams);

        setupMenuButtons();
    }

    private void setupMenuButtons() {
        Button exitButton = menuView.findViewById(R.id.exitButton);
        Button toggleButton = menuView.findViewById(R.id.toggleButton);
        Button undoButton = menuView.findViewById(R.id.undoButton);
        Button saveButton = menuView.findViewById(R.id.saveButton);

        exitButton.setOnClickListener(v -> {
            cleanup();
            finish();
        });

        toggleButton.setOnClickListener(v -> {
            isDrawingEnabled = !isDrawingEnabled;
            drawingView.setEnabled(isDrawingEnabled);
            toggleButton.setText(isDrawingEnabled ? "Disable" : "Enable");
        });

        undoButton.setOnClickListener(v -> drawingView.undo());

        saveButton.setOnClickListener(v -> saveScreenshot());
    }

    private void saveScreenshot() {
        // Implementation for saving screenshot
        // Will be added in DrawingView class
    }

    private void cleanup() {
        if (windowManager != null) {
            if (drawingView != null) {
                windowManager.removeView(drawingView);
            }
            if (menuView != null) {
                windowManager.removeView(menuView);
            }
        }
    }

    @Override
    protected void onDestroy() {
        cleanup();
        super.onDestroy();
    }
}