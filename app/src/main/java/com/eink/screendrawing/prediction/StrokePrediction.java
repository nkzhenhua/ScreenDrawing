package com.eink.screendrawing.prediction;

import android.graphics.Path;
import java.util.List;

public interface StrokePrediction {
    Path predictStroke(List<TouchPoint> history);
    void setPredictionTime(long ms);
    void setSpeedMultiplier(float multiplier);
} 