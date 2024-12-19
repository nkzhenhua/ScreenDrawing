package com.eink.screendrawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

// ... other imports
public class DrawingViewEInk extends View {
    // Constants
    private static final float STROKE_WIDTH = 5f;
    private static final int DEFAULT_COLOR = Color.BLACK;

    // optimize for eink to delay the redraw by distance change and time change
    // Adjust DISTANCE_THRESHOLD based on screen density
    float density = getResources().getDisplayMetrics().density;
    private static final float MIN_DISTANCE_THRESHOLD = 2f; // Minimum threshold in pixels
    private static final float MAX_DISTANCE_THRESHOLD = 5f; // Maximum threshold in pixels
    private static final long SPEED_CALCULATION_INTERVAL = 200; // Interval for speed calculation in ms
    private static final long UPDATE_INTERVAL = 50; // 50ms
    private float lastX, lastY;
    private long lastUpdateTime, lastSpeedCalculationTime;
    private float currentSpeed;

    private Path currentPath;
    private Paint paint;
    private List<PathData> pathDataList;

    public DrawingViewEInk(Context context) {
        super(context);
        init();
    }

    private void init() {
        pathDataList = new ArrayList<>();
        currentPath = new Path();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(DEFAULT_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(STROKE_WIDTH);

        pathDataList.add(new PathData(currentPath, new Paint(paint)));

        // Ensure transparent background
        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (PathData pathData : pathDataList) {
            canvas.drawPath(pathData.path, pathData.paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Return false immediately if disabled, allowing the event to pass through
        if (!isEnabled()) {
            return false;
        }

        // Get the pointer ID
        int pointerIndex = event.getActionIndex();
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                pathDataList.add(new PathData(currentPath, new Paint(paint)));
                lastX = x;
                lastY = y;
                lastUpdateTime = System.currentTimeMillis();
                currentSpeed = 0;
                invalidate(); // Invalidate immediately on touch down
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                long currentTime = System.currentTimeMillis();
                // Calculate drawing speed
                if (currentTime - lastSpeedCalculationTime >= SPEED_CALCULATION_INTERVAL) {
                    float distance = (float) Math.sqrt(Math.pow(x - lastX, 2) + Math.pow(y - lastY, 2));
                    float time = (currentTime - lastSpeedCalculationTime) / 1000f; // Convert to seconds
                    currentSpeed = distance / time;

                    lastX = x;
                    lastY = y;
                    lastSpeedCalculationTime = currentTime;
                }
                // Optimization 1: Distance threshold
                // Dynamically adjust DISTANCE_THRESHOLD
                float distanceThreshold = Math.max(MIN_DISTANCE_THRESHOLD, Math.min(MAX_DISTANCE_THRESHOLD, currentSpeed / 100f));
                float distance = (float) Math.sqrt(Math.pow(x - lastX, 2) + Math.pow(y - lastY, 2));
                if (distance >= distanceThreshold) {
                    lastX = x;
                    lastY = y;
                    invalidate();
                } else if (currentTime - lastUpdateTime >= UPDATE_INTERVAL){
                    // Optimization 2: Time interval
                    lastUpdateTime = currentTime;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                invalidate();
                break;
            default:
                return false;
        }
        return true;
    }

    public void undo() {
        if (!pathDataList.isEmpty()) {
            pathDataList.remove(pathDataList.size() - 1);
            invalidate();
        }
    }

    // Add this method to clear the canvas when disabled
    public void clearCanvas() {
        pathDataList.clear();
        currentPath = new Path();
        pathDataList.add(new PathData(currentPath, new Paint(paint)));
        invalidate();
    }

    // Optional: Add this method to handle enable/disable state changes
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            // Optionally clear the canvas when disabled
            // clearCanvas();  // Uncomment if you want to clear drawings when disabled
        }
    }

    public void setStrokeWidth(float width) {
        paint.setStrokeWidth(width);
    }
    
    public void setColor(int color) {
        paint.setColor(color);
    }
}