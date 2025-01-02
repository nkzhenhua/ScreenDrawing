package com.eink.screendrawing;

import android.view.View;
import com.eink.screendrawing.prediction.StrokePrediction;

public interface IDrawingView {
    void undo();
    void clearCanvas();
    void setEnabled(boolean enabled);
    void setStrokeWidth(float width);
    void setColor(int color);
    void setPredictionTime(long ms);
    void setPredictionAlgorithm(StrokePrediction predictor);
    View asView();  // 返回实际的View对象
} 