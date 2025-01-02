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
import android.widget.SeekBar;
import android.widget.TextView;


public class OverlayService extends Service {
    private static final String TAG = "OverlayService";
    private static final String PREFS_NAME = "DrawingPrefs";
    private static final String KEY_EINK_MODE = "isEinkMode";
    
    private WindowManager windowManager;
    private IDrawingView drawingView;
    private View menuView;
    private boolean isDrawingEnabled = false;
    private boolean isMenuCollapsed = false;
    private boolean isEinkMode;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; //milliseconds
    private WindowManager.LayoutParams drawParams;
    private WindowManager.LayoutParams menuParams;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Load saved preference
        isEinkMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_EINK_MODE, true);  // 默认为 true (EInk mode)
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
    }

    private void createOverlay() {
        // Create window parameters
        drawParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        // Initialize drawing view with saved preference
        createDrawingView(isEinkMode);
        windowManager.addView(drawingView.asView(), drawParams);

        // Initialize menu
        LayoutInflater inflater = LayoutInflater.from(this);
        menuView = inflater.inflate(R.layout.floating_menu, null);

        menuParams = new WindowManager.LayoutParams(
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
        
        // Set initial button text based on saved preference
        Button toggleViewButton = menuView.findViewById(R.id.toggleViewButton);
        toggleViewButton.setText(isEinkMode ? "EInk" : "NonE");
        
        setupMenuButtons(menuParams);
    }

    private void createDrawingView(boolean eInkMode) {
        drawingView = eInkMode ? new DrawingViewEInk(this) : new DrawingView(this);
    }

    private void switchDrawingView() {
        if (drawingView != null) {
            // Remove old view
            windowManager.removeView(drawingView.asView());
            
            // Create new view
            isEinkMode = !isEinkMode;
            // Save the new preference
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_EINK_MODE, isEinkMode)
                .apply();
            
            createDrawingView(isEinkMode);
            
            // Remove and re-add menu view to ensure it's on top
            windowManager.removeView(menuView);
            
            // Add new view
            windowManager.addView(drawingView.asView(), drawParams);
            windowManager.addView(menuView, menuParams);  // Re-add menu on top
            
            // Update button text
            Button toggleViewButton = menuView.findViewById(R.id.toggleViewButton);
            toggleViewButton.setText(isEinkMode ? "EInk" : "NonE");
            
            // Maintain enabled state
            drawingView.setEnabled(isDrawingEnabled);
            updateDrawingViewTouchability(isDrawingEnabled);
        }
    }

    private void setupMenuButtons(final WindowManager.LayoutParams menuParams) {
        Button exitButton = menuView.findViewById(R.id.exitButton);
        Button toggleButton = menuView.findViewById(R.id.toggleButton);
        Button undoButton = menuView.findViewById(R.id.undoButton);
        Button clearButton = menuView.findViewById(R.id.clearButton);
        Button toggleViewButton = menuView.findViewById(R.id.toggleViewButton);

        // Setup prediction time control
        SeekBar predictionSeekBar = menuView.findViewById(R.id.predictionSeekBar);
        TextView predictionValueText = menuView.findViewById(R.id.predictionValueText);
        
        predictionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (drawingView != null) {
                    drawingView.setPredictionTime(progress);
                    predictionValueText.setText(progress + " ms");
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Start with drawing disabled
        isDrawingEnabled = false;
        drawingView.setEnabled(isDrawingEnabled);
        updateDrawingViewTouchability(isDrawingEnabled);
        toggleButton.setText(isDrawingEnabled ? getString(R.string.stop) : getString(R.string.draw));

        exitButton.setOnClickListener(v -> stopSelf());

        toggleButton.setOnClickListener(v -> {
            isDrawingEnabled = !isDrawingEnabled;
            drawingView.setEnabled(isDrawingEnabled);
            toggleButton.setText(isDrawingEnabled ? getString(R.string.stop) : getString(R.string.draw));
            updateDrawingViewTouchability(isDrawingEnabled);
        });

        undoButton.setOnClickListener(v -> drawingView.undo());
        clearButton.setOnClickListener(v -> drawingView.clearCanvas());

        // Setup view toggle button
        toggleViewButton.setOnClickListener(v -> switchDrawingView());

        setupDragHandle(menuParams);
    }

    private void setupDragHandle(final WindowManager.LayoutParams menuParams) {
        View dragHandle = menuView.findViewById(R.id.dragHandle);
        final LinearLayout menuLayout = (LinearLayout) menuView;
        final int[] initialX = new int[1];
        final int[] initialY = new int[1];
        final float[] initialTouchX = new float[1];
        final float[] initialTouchY = new float[1];
        final int EDGE_THRESHOLD = 50;

        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Handle double click detection
                        long clickTime = System.currentTimeMillis();
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                            // Double click detected
                            toggleMenuButtons();
                            return true;
                        }
                        lastClickTime = clickTime;

                        // Normal drag initialization
                        initialX[0] = menuParams.x;
                        initialY[0] = menuParams.y;
                        initialTouchX[0] = event.getRawX();
                        initialTouchY[0] = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Only handle drag if it's not a double click
                        if (System.currentTimeMillis() - lastClickTime > 100) {
                            menuParams.x = initialX[0] + (int) (event.getRawX() - initialTouchX[0]);
                            menuParams.y = initialY[0] + (int) (event.getRawY() - initialTouchY[0]);

                            // Rest of your existing drag code...
                            DisplayMetrics metrics = getResources().getDisplayMetrics();
                            int screenWidth = metrics.widthPixels;
                            int screenHeight = metrics.heightPixels;

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
                        }
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
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) drawingView.asView().getLayoutParams();
        if (enabled) {
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        windowManager.updateViewLayout(drawingView.asView(), params);
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
                    windowManager.removeView(drawingView.asView());
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
    private void toggleMenuButtons() {
        isMenuCollapsed = !isMenuCollapsed;

        Button exitButton = menuView.findViewById(R.id.exitButton);
        Button toggleButton = menuView.findViewById(R.id.toggleButton);
        Button undoButton = menuView.findViewById(R.id.undoButton);
        Button clearButton = menuView.findViewById(R.id.clearButton);
        Button toggleViewButton = menuView.findViewById(R.id.toggleViewButton);

        int visibility = isMenuCollapsed ? View.GONE : View.VISIBLE;
        exitButton.setVisibility(visibility);
        toggleButton.setVisibility(visibility);
        undoButton.setVisibility(visibility);
        clearButton.setVisibility(visibility);
        toggleViewButton.setVisibility(visibility);
    }
}