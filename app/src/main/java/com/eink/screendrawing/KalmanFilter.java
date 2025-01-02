public class KalmanFilter {
    private float[] state; // [x, y]
    private float[][] covariance; // Covariance matrix
    private final float processNoise; // Process noise
    private final float measurementNoise; // Measurement noise

    public KalmanFilter(float processNoise, float measurementNoise) {
        this.state = new float[]{0, 0};
        this.covariance = new float[][]{{1, 0}, {0, 1}};
        this.processNoise = processNoise;
        this.measurementNoise = measurementNoise;
    }

    public void update(float x, float y) {
        // Prediction step
        covariance[0][0] += processNoise;
        covariance[1][1] += processNoise;

        // Kalman gain calculation
        float kx = covariance[0][0] / (covariance[0][0] + measurementNoise);
        float ky = covariance[1][1] / (covariance[1][1] + measurementNoise);

        // Update state with measurement
        state[0] += kx * (x - state[0]);
        state[1] += ky * (y - state[1]);

        // Update covariance matrix
        covariance[0][0] *= (1 - kx);
        covariance[1][1] *= (1 - ky);
    }

    public float[] predict() {
        // Return predicted state
        return new float[]{state[0], state[1]};
    }
}
