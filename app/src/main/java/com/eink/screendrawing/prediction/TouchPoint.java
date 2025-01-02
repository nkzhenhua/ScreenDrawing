package com.eink.screendrawing.prediction;

public class TouchPoint {
    public final float x;
    public final float y;
    public final float pressure;
    public final long timestamp;

    public TouchPoint(float x, float y, float pressure, long timestamp) {
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.timestamp = timestamp;
    }
} 