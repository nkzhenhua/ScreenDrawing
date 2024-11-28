package com.eink.screendrawing;

import android.graphics.Paint;
import android.graphics.Path;

class PathData {
    Path path;
    Paint paint;

    PathData(Path path, Paint paint) {
        this.path = path;
        this.paint = paint;
    }
}