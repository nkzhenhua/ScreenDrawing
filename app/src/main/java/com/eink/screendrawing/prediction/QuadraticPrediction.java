package com.eink.screendrawing.prediction;

import android.graphics.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuadraticPrediction implements StrokePrediction {
    private long predictionTime = 10; // ms
    private float speedMultiplier = 1.0f; // 速度系数，可调整

    @Override
    public Path predictStroke(List<TouchPoint> history) {
        if (history.size() < 3) return null;

        List<TouchPoint> recentPoints = getRecentPoints(history);
        if (recentPoints.size() < 3) return null;

        // 计算实际路径长度和速度
        float pathLength = 0;
        for (int i = 1; i < recentPoints.size(); i++) {
            TouchPoint current = recentPoints.get(i);
            TouchPoint prev = recentPoints.get(i-1);
            float segmentDx = current.x - prev.x;
            float segmentDy = current.y - prev.y;
            pathLength += Math.sqrt(segmentDx * segmentDx + segmentDy * segmentDy);
        }
        
        // 计算实际速度
        TouchPoint newest = recentPoints.get(recentPoints.size() - 1);
        TouchPoint oldest = recentPoints.get(0);
        float dt = newest.timestamp - oldest.timestamp; // 毫秒
        float speed = pathLength / dt; // pixels/ms

        // 根据速度和预测时间计算预测长度
        float predictionLength = speed * predictionTime * speedMultiplier;
        
        double[] xPoints = new double[recentPoints.size()];
        double[] yPoints = new double[recentPoints.size()];
        
        float baseX = recentPoints.get(0).x;
        float baseY = recentPoints.get(0).y;
        for (int i = 0; i < recentPoints.size(); i++) {
            TouchPoint point = recentPoints.get(i);
            xPoints[i] = point.x - baseX;
            yPoints[i] = point.y - baseY;
        }
        
        double[] params = fitQuadraticCurve(xPoints, yPoints);
        
        Path predictedPath = new Path();
        TouchPoint lastPoint = recentPoints.get(recentPoints.size() - 1);
        predictedPath.moveTo(lastPoint.x, lastPoint.y);
        
        float step = 2.0f;
        float currentDist = 0;
        float lastX = lastPoint.x;
        float lastY = lastPoint.y;
        
        while (currentDist < predictionLength) {
            float stepX = step;
            float stepY = (float)(params[0] * stepX * stepX + params[1] * stepX + params[2]);
            float newX = lastX + stepX;
            float newY = lastY + stepY;
            predictedPath.lineTo(newX, newY);
            
            float dx = newX - lastX;
            float dy = newY - lastY;
            currentDist += Math.sqrt(dx * dx + dy * dy);
            
            lastX = newX;
            lastY = newY;
        }
        
        return predictedPath;
    }

    private static final long HISTORY_DURATION = 5; // 5ms history duration
    
    private List<TouchPoint> getRecentPoints(List<TouchPoint> history) {
        long currentTime = System.currentTimeMillis();
        long threshold = currentTime - HISTORY_DURATION;
        return history.stream()
                     .filter(point -> point.timestamp >= threshold)
                     .collect(Collectors.toList());
    }

    private double[] fitQuadraticCurve(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumX2 = 0, sumX3 = 0, sumX4 = 0;
        double sumY = 0, sumXY = 0, sumX2Y = 0;
        
        for (int i = 0; i < n; i++) {
            double xi = x[i];
            double yi = y[i];
            double xi2 = xi * xi;
            
            sumX += xi;
            sumX2 += xi2;
            sumX3 += xi2 * xi;
            sumX4 += xi2 * xi2;
            sumY += yi;
            sumXY += xi * yi;
            sumX2Y += xi2 * yi;
        }
        
        double[][] matrix = {
            {sumX4, sumX3, sumX2},
            {sumX3, sumX2, sumX},
            {sumX2, sumX, n}
        };
        double[] vector = {sumX2Y, sumXY, sumY};
        
        return solveLinearSystem(matrix, vector);
    }

    private double[] solveLinearSystem(double[][] A, double[] b) {
        int n = A.length;
        for (int i = 0; i < n; i++) {
            // 找到主元
            int max = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(A[j][i]) > Math.abs(A[max][i])) {
                    max = j;
                }
            }
            
            // 交换行
            double[] temp = A[i];
            A[i] = A[max];
            A[max] = temp;
            double t = b[i];
            b[i] = b[max];
            b[max] = t;
            
            // 消元
            for (int j = i + 1; j < n; j++) {
                double factor = A[j][i] / A[i][i];
                b[j] -= factor * b[i];
                for (int k = i; k < n; k++) {
                    A[j][k] -= factor * A[i][k];
                }
            }
        }
        
        // 回代
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            x[i] = (b[i] - sum) / A[i][i];
        }
        
        return x;
    }

    @Override
    public void setSpeedMultiplier(float speedMultiplier) {
        // 改为设置速度系数
        this.speedMultiplier = speedMultiplier;
    }

    @Override
    public void setPredictionTime(long ms) {
        this.predictionTime = ms;
    }
} 