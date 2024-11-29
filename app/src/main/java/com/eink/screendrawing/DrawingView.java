package com.eink.screendrawing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// ... other imports
public class DrawingView extends View {
    // Constants
    private static final float STROKE_WIDTH = 12f;
    private static final int DEFAULT_COLOR = Color.BLACK;
    private Path currentPath;
    private Paint paint;
    private List<PathData> pathDataList;
    private Bitmap bitmap;
    private float currentStrokeWidth = STROKE_WIDTH;
    private int currentColor = DEFAULT_COLOR;

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

        pathDataList.add(new PathData(currentPath, new Paint(paint))); // Copy paint
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (bitmap != null) {
            bitmap.recycle(); // Properly recycle old bitmap
        }
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        }
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
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                // Handle cancellation if needed
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

    public boolean saveToGallery() {
        if (bitmap == null) {
            return false;
        }
        
        String fileName = "Screenshot_" + System.currentTimeMillis() + ".png";
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            
            // Notify gallery
            MediaScannerConnection.scanFile(getContext(),
                    new String[]{file.toString()},
                    null,
                    null);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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
        currentStrokeWidth = width;
        paint.setStrokeWidth(width);
    }
    
    public void setColor(int color) {
        currentColor = color;
        paint.setColor(color);
    }
}