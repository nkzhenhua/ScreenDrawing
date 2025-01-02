package com.eink.screendrawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import com.eink.screendrawing.prediction.TouchPoint;
import com.eink.screendrawing.prediction.StrokePrediction;
import com.eink.screendrawing.prediction.QuadraticPrediction;

public class DrawingViewEInk extends View {
    // Constants
    private static final float STROKE_WIDTH = 5f;
    private static final int DEFAULT_COLOR = Color.BLACK;

    private float lastX, lastY;
    private float lastPressure = 1.0f;

    private Path currentPath;
    private Path predictedPath;
    private Paint paint;
    private List<PathData> pathDataList;
    
    private List<TouchPoint> touchHistory;
    private StrokePrediction predictor;

    // Prediction related constants
    private static final long HISTORY_DURATION = 5;    // 保存过去5ms的历史数据

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
        
        touchHistory = new ArrayList<>();
        predictor = new QuadraticPrediction();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (PathData pathData : pathDataList) {
            canvas.drawPath(pathData.path, pathData.paint);
        }
        if(predictedPath != null){
            canvas.drawPath(predictedPath, paint);
        }
    }

    private void predictPath(float x, float y, float pressure) {
        predictedPath = predictor.predictStroke(touchHistory);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        int pointerIndex = event.getActionIndex();
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        float pressure = event.getPressure(pointerIndex);
        pressure = pressure > 0 ? pressure : lastPressure;
        lastPressure = pressure;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                pathDataList.add(new PathData(currentPath, new Paint(paint)));
                lastX = x;
                lastY = y;
                touchHistory.clear();
                touchHistory.add(new TouchPoint(x, y, pressure, System.currentTimeMillis()));
                predictedPath = null;
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                long currentTime = System.currentTimeMillis();
                TouchPoint newTouch = new TouchPoint(x, y, pressure, currentTime);
                touchHistory.add(newTouch);
                
                // 只保留最近HISTORY_DURATION时间内的历史数据
                long threshold = currentTime - HISTORY_DURATION;
                while (!touchHistory.isEmpty() && touchHistory.get(0).timestamp < threshold) {
                    touchHistory.remove(0);
                }
                
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                
                predictPath(x, y, pressure);
                lastX = x;
                lastY = y;
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                predictedPath = null;
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
            predictedPath = null;
            touchHistory.clear();
            invalidate();
        }
    }

    public void clearCanvas() {
        pathDataList.clear();
        currentPath = new Path();
        pathDataList.add(new PathData(currentPath, new Paint(paint)));
        predictedPath = null;
        touchHistory.clear();
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

    public void setPredictionTime(long ms) {
        if (predictor != null) {
            predictor.setPredictionTime(ms);  // 更新预测器中的时间
        }
    }

    public void setPredictionAlgorithm(StrokePrediction predictor) {
        this.predictor = predictor;
    }
}