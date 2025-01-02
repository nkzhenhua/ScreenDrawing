package com.eink.screendrawing;

class TouchData {
    float x, y;
    float pressure;
    long timestamp;
    
    TouchData(float x, float y, float pressure, long timestamp) {
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.timestamp = timestamp;
    }
} 