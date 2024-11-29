package com.eink.screendrawing;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private WindowManager windowManager;
    private DrawingView drawingView;
    private View menuView;
    private boolean isDrawingEnabled = true;
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make activity window transparent and non-interactive
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);  // Optional: for edge-to-edge support
        
        // Make window transparent and pass-through
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Register the launcher before checking permission
        overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Settings.canDrawOverlays(this)) {
                    initializeOverlay();
                } else {
                    // Permission denied, handle accordingly
                    Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        );

        // Check for overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(drawingView, drawParams);
        
        // Set initial state
        updateDrawingViewTouchability(isDrawingEnabled);

        // Initialize menu
        initializeMenu();
    }

    private void updateDrawingViewTouchability(boolean enabled) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) drawingView.getLayoutParams();
        if (enabled) {
            // Remove NOT_TOUCHABLE flag when enabled
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            // Add NOT_TOUCHABLE flag when disabled
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        try {
            windowManager.updateViewLayout(drawingView, params);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
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
        
        // Add initial position if desired
        menuParams.x = 100;
        menuParams.y = 100;

        windowManager.addView(menuView, menuParams);
        setupMenuButtons();
        
        // Make menu draggable
        View dragHandle = menuView.findViewById(R.id.dragHandle);
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = menuParams.x;
                        initialY = menuParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        menuParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        menuParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(menuView, menuParams);
                        return true;
                }
                return false;
            }
        });
    }

    private void setupMenuButtons() {
        Button exitButton = menuView.findViewById(R.id.exitButton);
        Button toggleButton = menuView.findViewById(R.id.toggleButton);
        Button undoButton = menuView.findViewById(R.id.undoButton);
        Button saveButton = menuView.findViewById(R.id.saveButton);
        Button clearButton = menuView.findViewById(R.id.clearButton);

        exitButton.setOnClickListener(v -> {
            cleanup();
            finish();
        });

        toggleButton.setText(isDrawingEnabled ? R.string.disable : R.string.enable);

        toggleButton.setOnClickListener(v -> {
            isDrawingEnabled = !isDrawingEnabled;
            drawingView.setEnabled(isDrawingEnabled);
            updateDrawingViewTouchability(isDrawingEnabled);
            toggleButton.setText(isDrawingEnabled ? R.string.disable : R.string.enable);
        });

        undoButton.setOnClickListener(v -> drawingView.undo());

        saveButton.setOnClickListener(v -> saveScreenshot());

        clearButton.setOnClickListener(v -> {
            if (drawingView != null) {
                drawingView.clearCanvas();
            }
        });
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