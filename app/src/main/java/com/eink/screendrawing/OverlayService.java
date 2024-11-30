package com.eink.screendrawing;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

// Your custom view imports
import com.eink.screendrawing.DrawingView;
import com.eink.screendrawing.MainActivity;
import com.eink.screendrawing.R;

public class OverlayService extends Service {
    private static final String TAG = "OverlayService";
    
    private WindowManager windowManager;
    private DrawingView drawingView;
    private View menuView;
    private boolean isDrawingEnabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
    }

    private void createOverlay() {
        // Initialize drawing view
        drawingView = new DrawingView(this);
        WindowManager.LayoutParams drawParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        windowManager.addView(drawingView, drawParams);

        // Initialize menu
        LayoutInflater inflater = LayoutInflater.from(this);
        menuView = inflater.inflate(R.layout.floating_menu, null);

        WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.TOP | Gravity.LEFT;
        menuParams.x = 0;
        menuParams.y = 0;

        windowManager.addView(menuView, menuParams);
        setupMenuButtons(menuParams);
    }

    private void setupMenuButtons(final WindowManager.LayoutParams menuParams) {
        Button exitButton = menuView.findViewById(R.id.exitButton);
        Button toggleButton = menuView.findViewById(R.id.toggleButton);
        Button undoButton = menuView.findViewById(R.id.undoButton);
        Button clearButton = menuView.findViewById(R.id.clearButton);

        // Start with drawing disabled
        isDrawingEnabled = false;
        drawingView.setEnabled(isDrawingEnabled);
        updateDrawingViewTouchability(isDrawingEnabled);
        toggleButton.setText(isDrawingEnabled ? "✎ STOP" : "✎ DRAW");

        exitButton.setOnClickListener(v -> stopSelf());

        toggleButton.setOnClickListener(v -> {
            isDrawingEnabled = !isDrawingEnabled;
            drawingView.setEnabled(isDrawingEnabled);
            toggleButton.setText(isDrawingEnabled ? "✎ STOP" : "✎ DRAW");
            updateDrawingViewTouchability(isDrawingEnabled);
        });

        undoButton.setOnClickListener(v -> drawingView.undo());
        clearButton.setOnClickListener(v -> drawingView.clearCanvas());

        setupDragHandle(menuParams);
    }

    private void setupDragHandle(final WindowManager.LayoutParams menuParams) {
        View dragHandle = menuView.findViewById(R.id.dragHandle);
        final LinearLayout menuLayout = (LinearLayout) menuView;
        final int[] initialX = new int[1];
        final int[] initialY = new int[1];
        final float[] initialTouchX = new float[1];
        final float[] initialTouchY = new float[1];
        final int EDGE_THRESHOLD = 100;

        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX[0] = menuParams.x;
                        initialY[0] = menuParams.y;
                        initialTouchX[0] = event.getRawX();
                        initialTouchY[0] = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        menuParams.x = initialX[0] + (int) (event.getRawX() - initialTouchX[0]);
                        menuParams.y = initialY[0] + (int) (event.getRawY() - initialTouchY[0]);
                        
                        // Get screen dimensions using non-deprecated method
                        DisplayMetrics metrics = getResources().getDisplayMetrics();
                        int screenWidth = metrics.widthPixels;
                        int screenHeight = metrics.heightPixels;

                        // Check position and update orientation
                        if (menuParams.x < EDGE_THRESHOLD || screenWidth - menuParams.x < EDGE_THRESHOLD) {
                            if (menuLayout.getOrientation() != LinearLayout.VERTICAL) {
                                menuLayout.setOrientation(LinearLayout.VERTICAL);
                                updateDragHandleForOrientation(true);
                            }
                        } else if (menuParams.y < EDGE_THRESHOLD || screenHeight - menuParams.y < EDGE_THRESHOLD) {
                            if (menuLayout.getOrientation() != LinearLayout.HORIZONTAL) {
                                menuLayout.setOrientation(LinearLayout.HORIZONTAL);
                                updateDragHandleForOrientation(false);
                            }
                        }

                        windowManager.updateViewLayout(menuView, menuParams);
                        return true;
                }
                return false;
            }
        });
    }

    private void updateDragHandleForOrientation(boolean isVertical) {
        TextView dragHandleText = menuView.findViewById(R.id.dragHandleText);
        if (isVertical) {
            dragHandleText.setText("⠿ ⠿ ⠿"); // Three groups for vertical
        } else {
            dragHandleText.setText("⠿ ⠿"); // Two groups for horizontal
        }
    }

    private void updateDrawingViewTouchability(boolean enabled) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) drawingView.getLayoutParams();
        if (enabled) {
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        windowManager.updateViewLayout(drawingView, params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null) {
            if (drawingView != null) {
                try {
                    windowManager.removeView(drawingView);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Error removing drawing view: " + e.getMessage());
                }
            }
            if (menuView != null) {
                try {
                    windowManager.removeView(menuView);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Error removing menu view: " + e.getMessage());
                }
            }
        }
    }
}