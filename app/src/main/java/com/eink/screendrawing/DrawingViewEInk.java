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

public class DrawingViewEInk extends View {
    // Constants
    private static final float STROKE_WIDTH = 5f;
    private static final int DEFAULT_COLOR = Color.BLACK;

    private float lastX, lastY;

    private Path currentPath;
    private Path predictedPath;
    private Paint paint;
    private List<PathData> pathDataList;
    private List<TouchData> touchHistory = new ArrayList<>();

    // Prediction related constants
    private static final long HISTORY_DURATION = 5;    // 保存过去5ms的历史数据
    private long predictionTime = 17;                   // 预测和计算速度的时间窗口(ms)
    private static final float PREDICTION_CURVE_RATIO = 0.3f; // 贝塞尔曲线控制点比例

    private float lastPressure = 1.0f;

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
        if (touchHistory.size() < 2) return;
        
        long currentTime = System.currentTimeMillis();
        long velocityThreshold = currentTime - predictionTime;
        
        // 获取用于计算速度的最近数据点
        List<TouchData> velocityPoints = new ArrayList<>();
        for (int i = touchHistory.size() - 1; i >= 0; i--) {
            TouchData point = touchHistory.get(i);
            if (point.timestamp >= velocityThreshold) {
                velocityPoints.add(0, point);
            } else {
                break;
            }
        }
        
        if (velocityPoints.size() < 2) return;
        
        // 计算最近predictionTime时间内的平均速度
        TouchData newest = velocityPoints.get(velocityPoints.size() - 1);
        TouchData oldest = velocityPoints.get(0);
        
        float timeSpan = (newest.timestamp - oldest.timestamp);
        if (timeSpan <= 0) return;
        
        // 计算速度向量 (像素/毫秒)
        float vx = (newest.x - oldest.x) / timeSpan;
        float vy = (newest.y - oldest.y) / timeSpan;
        
        // 预测未来predictionTime时间的位置
        float predictedX = newest.x + vx * predictionTime;
        float predictedY = newest.y + vy * predictionTime;
        
        // 创建预测路径
        predictedPath = new Path();
        predictedPath.moveTo(newest.x, newest.y);
        
        // 使用三阶贝塞尔曲线创建平滑的预测路径
        float controlLen = predictionTime * PREDICTION_CURVE_RATIO;
        predictedPath.cubicTo(
            newest.x + vx * controlLen,
            newest.y + vy * controlLen,
            predictedX - vx * controlLen,
            predictedY - vy * controlLen,
            predictedX,
            predictedY
        );
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
                touchHistory.add(new TouchData(x, y, pressure, System.currentTimeMillis()));
                predictedPath = null;
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                long currentTime = System.currentTimeMillis();
                TouchData newTouch = new TouchData(x, y, pressure, currentTime);
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
        this.predictionTime = ms;
    }
}