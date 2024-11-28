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
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
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
        if (!isEnabled()) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                currentPath = new Path();
                pathDataList.add(new PathData(currentPath, new Paint(paint)));
                break;
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

    public void saveToGallery() {
        String fileName = "Screenshot_" + System.currentTimeMillis() + ".png";
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            // Notify gallery
            MediaScannerConnection.scanFile(getContext(),
                    new String[]{file.toString()},
                    null,
                    null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}