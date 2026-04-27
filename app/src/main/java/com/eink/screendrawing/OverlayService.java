package com.eink.screendrawing;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

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
    private boolean isMenuCollapsed = false;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; //milliseconds

    // UI 元素引用
    private FrameLayout btnDraw;
    private ImageView iconDraw;
    private FrameLayout btnUndo;
    private FrameLayout btnClear;
    private FrameLayout btnExit;
    private View dividerView;

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
        android.view.ContextThemeWrapper themeContext = new android.view.ContextThemeWrapper(this, R.style.Theme_ScreenDrawing);
        LayoutInflater inflater = LayoutInflater.from(themeContext);
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
        // 获取新的 UI 元素引用
        btnDraw = menuView.findViewById(R.id.btnDraw);
        iconDraw = menuView.findViewById(R.id.iconDraw);
        btnUndo = menuView.findViewById(R.id.btnUndo);
        btnClear = menuView.findViewById(R.id.btnClear);
        btnExit = menuView.findViewById(R.id.btnExit);
        dividerView = menuView.findViewById(R.id.dividerView);

        // Start with drawing disabled
        isDrawingEnabled = false;
        drawingView.setEnabled(isDrawingEnabled);
        updateDrawingViewTouchability(isDrawingEnabled);
        setDrawingMode(isDrawingEnabled);

        btnExit.setOnClickListener(v -> stopSelf());

        btnDraw.setOnClickListener(v -> {
            isDrawingEnabled = !isDrawingEnabled;
            drawingView.setEnabled(isDrawingEnabled);
            setDrawingMode(isDrawingEnabled);
            updateDrawingViewTouchability(isDrawingEnabled);
        });

        btnUndo.setOnClickListener(v -> drawingView.undo());
        btnClear.setOnClickListener(v -> drawingView.clearCanvas());

        setupDragHandle(menuParams);
    }

    /**
     * 设置画笔按钮的激活/非激活外观
     */
    private void setDrawingMode(boolean isOn) {
        if (isOn) {
            // 开启状态：蓝色背景 + 白色图标
            btnDraw.setBackgroundResource(R.drawable.bg_active_brush);
            iconDraw.setColorFilter(ContextCompat.getColor(this, R.color.icon_default_tint), PorterDuff.Mode.SRC_IN);
        } else {
            // 关闭状态：无背景 + 灰色图标
            btnDraw.setBackgroundResource(android.R.color.transparent);
            iconDraw.setColorFilter(ContextCompat.getColor(this, R.color.icon_disabled_tint), PorterDuff.Mode.SRC_IN);
        }
    }

    private void setupDragHandle(final WindowManager.LayoutParams menuParams) {
        View dragHandle = menuView.findViewById(R.id.dragHandle);
        final int[] initialX = new int[1];
        final int[] initialY = new int[1];
        final float[] initialTouchX = new float[1];
        final float[] initialTouchY = new float[1];

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
                            windowManager.updateViewLayout(menuView, menuParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        return true;
                }
                return false;
            }
        });
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

    private void toggleMenuButtons() {
        isMenuCollapsed = !isMenuCollapsed;

        // Set visibility for all buttons
        int visibility = isMenuCollapsed ? View.GONE : View.VISIBLE;
        btnDraw.setVisibility(visibility);
        btnUndo.setVisibility(visibility);
        btnClear.setVisibility(visibility);
        btnExit.setVisibility(visibility);
        if (dividerView != null) {
            dividerView.setVisibility(visibility);
        }
    }
}
