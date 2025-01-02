package com.eink.screendrawing;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import com.eink.screendrawing.prediction.StrokePrediction;

// ... other imports
public class DrawingView extends View implements IDrawingView {
    // Constants
    private static final float STROKE_WIDTH = 5f;
    private static final int DEFAULT_COLOR = Color.BLACK;
    private Path currentPath;
    private Paint paint;
    private List<PathData> pathDataList;

    public DrawingView(Context context) {
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
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                break;
            default:
                return false;
        }

        invalidate();
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

    @Override
    public void setPredictionTime(long ms) {
        // 普通DrawingView不需要实现预测功能
    }

    @Override
    public void setPredictionAlgorithm(StrokePrediction predictor) {
        // 普通DrawingView不需要实现预测功能
    }

    @Override
    public View asView() {
        return this;
    }
}